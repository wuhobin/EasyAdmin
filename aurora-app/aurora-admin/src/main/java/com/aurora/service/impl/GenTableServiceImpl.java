package com.aurora.service.impl;

import com.aurora.config.VelocityInitializer;
import com.aurora.service.GenTableService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.entity.GenTable;
import com.aurora.entity.GenTableColumn;
import com.aurora.mapper.GenTableMapper;
import com.aurora.common.Constants;
import com.aurora.utils.GenUtils;
import com.aurora.utils.VelocityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.io.FileUtils;
import java.io.IOException;
import com.aurora.exception.BusinessException;

import java.io.File;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

@Service
@RequiredArgsConstructor
public class GenTableServiceImpl implements GenTableService {

    private final GenTableMapper genTableMapper;

    @Override
    public Map<String, Object> selectGenTableList(GenTable genTable) {
        List<GenTable> list = genTableMapper.selectGenTableList(genTable);
        Map<String, Object> result = new HashMap<>();
        result.put("rows", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public Map<String, String> previewCode(Long tableId) {
        Map<String, String> dataMap = new HashMap<>();
        GenTable table = genTableMapper.selectGenTableById(tableId);
        List<GenTableColumn> columns = genTableMapper.selectGenTableColumns(tableId);

        for (GenTableColumn column : columns)
        {
            if (column.getIsPk() != null && "1".equals(column.getIsPk()))
            {
                table.setPkColumn(column);
                break;
            }
        }
        VelocityContext context = VelocityUtils.prepareContext(table, columns);

        // 获取模板列表
        List<String> templates = getTemplateList();
        for (String template : templates) {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);
            dataMap.put(getTemplateName(template), sw.toString());
        }

        return dataMap;
    }

    @Override
    @Transactional
    public int deleteGenTableByIds(Long[] tableIds) {
        genTableMapper.deleteGenTableColumnsByTableIds(tableIds);
        return genTableMapper.deleteGenTableByIds(tableIds);

    }

    @Override
    public void generatorCode(String tables) {
        String[] tableNames = tables.split(",");
        GenTable genTable = new GenTable();
        for (String tableName : tableNames) {
            // 查询表信息
            genTable.setTableName(tableName);
            List<GenTable> tableList = genTableMapper.selectDbTableList(genTable);
            GenTable table = tableList.get(0);
            // 查询列信息
            List<GenTableColumn> columns = genTableMapper.selectDbTableColumns(tableName);
            // 生成代码文件
            generateFiles(table, columns);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String synchDb(String tableName) {
        try {
            // 查询表信息
            GenTable genTable = new GenTable();
            genTable.setTableName(tableName);
            List<GenTable> tableList = genTableMapper.selectDbTableList(genTable);
            if (tableList.isEmpty()) {
                throw new BusinessException("同步数据失败，原表结构不存在");
            }
            GenTable table = tableList.get(0);

            // 查询原表列信息
            List<GenTableColumn> dbColumns = genTableMapper.selectDbTableColumns(tableName);
            if (dbColumns.isEmpty()) {
                throw new BusinessException("同步数据失败，原表结构不存在");
            }

            // 查询当前已生成的列信息
            GenTable oldTable = genTableMapper.selectGenTableByName(tableName);
            List<GenTableColumn> oldColumns = genTableMapper.selectGenTableColumns(oldTable.getTableId());

            // 处理列信息
            List<String> dbColumnNames = dbColumns.stream().map(GenTableColumn::getColumnName).collect(Collectors.toList());
            List<String> oldColumnNames = oldColumns.stream().map(GenTableColumn::getColumnName).collect(Collectors.toList());

            // 删除已删除的列
            List<GenTableColumn> delColumns = oldColumns.stream()
                    .filter(column -> !dbColumnNames.contains(column.getColumnName()))
                    .collect(Collectors.toList());
            if (!delColumns.isEmpty()) {
                genTableMapper.deleteGenTableColumns(delColumns.stream()
                        .map(GenTableColumn::getColumnId)
                        .collect(Collectors.toList()));
            }

            // 新增新增的列
            List<GenTableColumn> addColumns = dbColumns.stream()
                    .filter(column -> !oldColumnNames.contains(column.getColumnName()))
                    .collect(Collectors.toList());
            addColumns.forEach(column -> {
                initColumnField(column, oldTable);
            });
            if (!addColumns.isEmpty()) {
                genTableMapper.insertGenTableBatch(addColumns);
            }

            // 更新修改的列
            List<GenTableColumn> updateColumns = dbColumns.stream()
                    .filter(column -> oldColumnNames.contains(column.getColumnName()))
                    .collect(Collectors.toList());
            updateColumns.forEach(column -> {
                GenTableColumn oldColumn = oldColumns.stream()
                        .filter(c -> c.getColumnName().equals(column.getColumnName()))
                        .findFirst()
                        .orElse(null);
                if (oldColumn != null) {
                    column.setColumnId(oldColumn.getColumnId());
                    column.setTableId(oldTable.getTableId());
                    // 保持已有的配置信息
                    column.setIsList(oldColumn.getIsList());
                    column.setIsQuery(oldColumn.getIsQuery());
                    column.setIsEdit(oldColumn.getIsEdit());
                    column.setIsInsert(oldColumn.getIsInsert());
                    column.setQueryType(oldColumn.getQueryType());
                    column.setHtmlType(oldColumn.getHtmlType());
                    // 更新数据类型相关的信息
                    column.setJavaType(GenUtils.getJavaType(column.getColumnType()));
                    column.setJavaField(GenUtils.toCamelCase(column.getColumnName()));
                }
            });
            if (!updateColumns.isEmpty()) {
                genTableMapper.updateGenTableColumns(updateColumns);
            }

            // 更新表信息
            oldTable.setTableComment(table.getTableComment());
            oldTable.setUpdateTime(new Date());
            genTableMapper.updateGenTable(oldTable);

            return "同步成功";
        } catch (Exception e) {
            throw new BusinessException("同步失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> selectDbTableList(GenTable genTable, PageParam pageParam) {
        // 计算偏移量
        int pageNum = pageParam.getPageNum() != null ? pageParam.getPageNum() : 1;
        int pageSize = pageParam.getPageSize() != null ? pageParam.getPageSize() : 10;
        int offset = (pageNum - 1) * pageSize;
        // 设置分页参数
        genTable.setOffset(offset);
        genTable.setPageSize(pageSize);

        // 查询数据
        List<GenTable> list = genTableMapper.selectDbTableList(genTable);
        // 查询总数
        int total = genTableMapper.selectDbTableCount(genTable);

        Map<String, Object> result = new HashMap<>();
        result.put("rows", list);
        result.put("total", total);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importGenTable(String[] tableNames) {
        try {
            GenTable genTable = new GenTable();
            for (String tableName : tableNames) {
                genTable.setTableName(tableName);
                // 查询表信息
                List<GenTable> tableList = genTableMapper.selectDbTableList(genTable);
                if (tableList.isEmpty()) {
                    throw new BusinessException("表" + tableName + "不存在");
                }
                GenTable table = tableList.get(0);

                // 保存表信息
                genTableMapper.insertGenTable(table);

                // 查询列信息
                List<GenTableColumn> columns = genTableMapper.selectDbTableColumns(tableName);
                for (GenTableColumn column : columns) {
                    // 设置列的默认属性
                    initColumnField(column, table);
                }

                // 批量保存列信息
                if (!columns.isEmpty()) {
                    genTableMapper.insertGenTableBatch(columns);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("导入失败：" + e.getMessage());
        }
    }

    /**
     * 初始化列属性字段
     */
    private void initColumnField(GenTableColumn column, GenTable table) {
        String dataType = column.getColumnType();
        String columnName = column.getColumnName();

        // 设置java字段名
        column.setTableId(table.getTableId());
        column.setJavaField(GenUtils.toCamelCase(columnName));

        // 设置默认类型
        column.setJavaType(GenUtils.getJavaType(dataType));

        // 设置默认显示类型
        column.setQueryType("EQ");
        column.setHtmlType("input");

        // 主键字段设置
        if ("PRI".equals(column.getColumnKey())) {
            column.setIsPk("1");
            column.setIsRequired("1");
            column.setIsInsert("1");
            table.setPkColumn(column);
        } else {
            column.setIsPk("0");
            column.setIsRequired("0");
        }

        // 设置默认操作类型
        column.setIsInsert("1");
        column.setIsEdit("1");
        column.setIsList("1");
        column.setIsQuery("1");
    }

    private List<String> getTemplateList() {
        List<String> templates = new ArrayList<>();
        templates.add("templates/entity.java.vm");
        templates.add("templates/mapper.java.vm");
        templates.add("templates/service.java.vm");
        templates.add("templates/serviceImpl.java.vm");
        templates.add("templates/controller.java.vm");
        templates.add("templates/mapper.xml.vm");
        templates.add("templates/vue.vue.vm");
        return templates;
    }

    private String getTemplateName(String template) {
        String[] arr = template.split("/");
        String name = arr[arr.length - 1];
        return name.substring(0, name.indexOf(".vm"));
    }

    private void generateFiles(GenTable table, List<GenTableColumn> columns) {
        try {
            VelocityContext context = VelocityUtils.prepareContext(table, columns);

            // 获取模板列表
            List<String> templates = getTemplateList();
            for (String template : templates) {
                // 渲染模板
                StringWriter sw = new StringWriter();
                Template tpl = Velocity.getTemplate(template, "UTF-8");
                tpl.merge(context, sw);

                // 生成文件
                String path = getGeneratePath(template, table);
                if (path != null) {
                    File file = new File(path);
                    FileUtils.forceMkdirParent(file); // 确保父目录存在
                    FileUtils.writeStringToFile(file, sw.toString(), "UTF-8");
                }
            }
        } catch (IOException e) {
            throw new BusinessException("生成代码失败，请检查目录权限！", e);
        }
    }

    private String getGeneratePath(String template, GenTable table) {
        String className = VelocityUtils.convertClassName(table.getTableName());
        String packagePath = "src/main/java/com/neat/demo/";
        String resourcePath = "src/main/resources/";

        if (template.contains("entity.java.vm")) {
            return packagePath + "entity/" + className + ".java";
        } else if (template.contains("mapper.java.vm")) {
            return packagePath + "mapper/" + className + "Mapper.java";
        } else if (template.contains("service.java.vm")) {
            return packagePath + "service/" + className + "Service.java";
        } else if (template.contains("serviceImpl.java.vm")) {
            return packagePath + "service/impl/" + className + "ServiceImpl.java";
        } else if (template.contains("controller.java.vm")) {
            return packagePath + "controller/" + className + "Controller.java";
        } else if (template.contains("mapper.xml.vm")) {
            return resourcePath + "mapper/" + className + "Mapper.xml";
        } else if (template.contains("vue.vue.vm")) {
            return "vue/" + VelocityUtils.convertToCamelCase(table.getTableName()) + "/index.vue";
        }
        return null;
    }

    @Override
    public byte[] downloadCode(String[] tableNames) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);

        try {
            for (String tableName : tableNames) {
                // 查询表信息
                GenTable table = genTableMapper.selectGenTableByName(tableName);
                if (table == null) {
                    throw new RuntimeException("表" + tableName + "不存在");
                }

                // 查询列信息
                List<GenTableColumn> columns = genTableMapper.selectGenTableColumns(table.getTableId());

                // 生成代码
                VelocityInitializer.initVelocity();
                VelocityContext context = VelocityUtils.prepareContext(table, columns);

                // 获取模板列表
                List<String> templates = getTemplateList();
                for (String template : templates) {
                    // 渲染模板
                    StringWriter sw = new StringWriter();
                    Template tpl = Velocity.getTemplate(template, Constants.UTF8);
                    tpl.merge(context, sw);

                    // 获取生成的文件名
                    String fileName = getFileName(template, table);
                    if (fileName != null) {
                        // 添加到zip
                        zip.putNextEntry(new ZipEntry(fileName));
                        IOUtils.write(sw.toString(), zip, Constants.UTF8);
                        zip.closeEntry();
                    }
                }
            }

            zip.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("生成代码失败", e);
        } finally {
            IOUtils.closeQuietly(zip);
            IOUtils.closeQuietly(outputStream);
        }
    }

    private String getFileName(String template, GenTable table) {
        String className = table.getClassName();
        String packageName = table.getPackageName().replace(".", "/");

        if (template.contains("entity.java.vm")) {
            return packageName + "/entity/" + className + ".java";
        } else if (template.contains("mapper.java.vm")) {
            return packageName + "/mapper/" + className + "Mapper.java";
        } else if (template.contains("service.java.vm")) {
            return packageName + "/service/" + className + "Service.java";
        } else if (template.contains("serviceImpl.java.vm")) {
            return packageName + "/service/impl/" + className + "ServiceImpl.java";
        } else if (template.contains("controller.java.vm")) {
            return packageName + "/controller/" + className + "Controller.java";
        } else if (template.contains("mapper.xml.vm")) {
            return "mapper/" + className + "Mapper.xml";
        } else if (template.contains("vue.vue.vm")) {
            return "vue/" + className.toLowerCase() + "/index.vue";
        }
        return null;
    }

}

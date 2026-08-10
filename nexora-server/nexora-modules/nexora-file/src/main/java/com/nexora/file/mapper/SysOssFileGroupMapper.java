package com.nexora.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexora.file.entity.SysOssFileGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysOssFileGroupMapper extends BaseMapper<SysOssFileGroup> {

    @Select("""
            SELECT g.id,
                   g.owner_id,
                   g.name,
                   g.create_time,
                   g.update_time,
                   COUNT(f.id) AS file_count
            FROM sys_oss_file_group g
            LEFT JOIN sys_oss_file f ON f.group_id = g.id AND f.uploader_id = g.owner_id
            WHERE g.owner_id = #{ownerId}
            GROUP BY g.id, g.owner_id, g.name, g.create_time, g.update_time
            ORDER BY g.name ASC, g.id ASC
            """)
    List<SysOssFileGroup> selectByOwnerId(@Param("ownerId") Long ownerId);

    @Select("SELECT COUNT(*) FROM sys_oss_file WHERE uploader_id = #{ownerId} AND group_id IS NULL")
    Long countUngrouped(@Param("ownerId") Long ownerId);
}

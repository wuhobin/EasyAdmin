package com.nexora.file.mapper;

import com.nexora.file.entity.SysOssFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysOssFileMapper extends BaseMapper<SysOssFile> {

    @Update("""
            UPDATE sys_oss_file
            SET original_filename = #{originalFilename},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND uploader_id = #{uploaderId}
            """)
    int updateOriginalFilename(@Param("id") Long id,
                               @Param("uploaderId") Long uploaderId,
                               @Param("originalFilename") String originalFilename);

    @Update("""
            <script>
            UPDATE sys_oss_file
            SET group_id = #{groupId},
                update_time = CURRENT_TIMESTAMP
            WHERE uploader_id = #{uploaderId}
              AND id IN
              <foreach collection="fileIds" item="fileId" open="(" separator="," close=")">
                  #{fileId}
              </foreach>
            </script>
            """)
    int updateGroup(@Param("fileIds") List<Long> fileIds,
                    @Param("uploaderId") Long uploaderId,
                    @Param("groupId") Long groupId);
}

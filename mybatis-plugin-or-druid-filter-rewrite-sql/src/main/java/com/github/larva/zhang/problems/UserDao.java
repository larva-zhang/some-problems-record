package com.github.larva.zhang.problems;

import java.util.Date;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * UserDao
 *
 * @author zhanghan
 * @date 2019/12/12
 * @since 1.0
 */
@Mapper
public interface UserDao {

    /**
     * 插入记录并且不包含更新时间
     *
     * @param account
     * @param name
     * @param age
     * @param sex
     * @param createTime
     * @return
     */
    @Insert("insert into `test_user`(`account`, `user_name`, `age`, `sex`, `create_time`)\n"
            + "values (#{account} , #{name} , #{age} , #{sex}, #{createTime}) on duplicate key update \n"
            + "`user_name` = #{name} , `age` = #{age} , `sex` = #{sex} ")
    int insertWithoutUpdateTime(@Param("account") String account, @Param("name") String name, @Param("age") int age,
            @Param("sex") boolean sex, @Param("createTime") Date createTime);

    /**
     * 插入记录并且包含更新时间
     *
     * @param account
     * @param name
     * @param age
     * @param sex
     * @param createTime
     * @param updateTime
     * @return
     */
    @Insert("insert into `test_user`(`account`, `user_name`, `age`, `sex`, `create_time`, `update_time`)\n"
            + "values (#{account} , #{name} , #{age} , #{sex}, #{createTime}, #{updateTime}) on duplicate key update \n"
            + "`user_name` = #{name} , `age` = #{age} , `sex` = #{sex}, `update_time` = #{updateTime} ")
    int insertWithUpdateTime(@Param("account") String account, @Param("name") String name, @Param("age") int age,
            @Param("sex") boolean sex, @Param("createTime") Date createTime, @Param("updateTime") Date updateTime);

    /**
     * 根据账号查询更新时间
     *
     * @param account
     * @return
     */
    @Select("select update_time from `test_user` where `account` = #{account} ")
    Date selectUpdateTimeByAccount(@Param("account") String account);
}

package com.zxl.problems.migratespringboot.dao;

/**
 * UsersDao
 *
 * @author zhanghan
 * @date 2018/12/5
 * @since 1.0
 */
public interface UsersDao {

	/**
	 * 判断是否存在用户名和密码对
	 *
	 * @param username
	 * @param token
	 * @return
	 */
	boolean isExistsUsernameTokenPair(String username, String token);
}

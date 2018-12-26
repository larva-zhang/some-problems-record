package com.zxl.problems.migratespringboot.service;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import com.zxl.problems.migratespringboot.dao.UsersDao;

/**
 * LoginService
 *
 * @author zhanghan
 * @date 2018/12/5
 * @since 1.0
 */
@Service
public class LoginService {

    @Resource
    private UsersDao usersDao;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean login(@Nonnull String username, @Nonnull char[] token) {
        String md5Token = DigestUtils.md5DigestAsHex(convertCharArrayToByteArrayWithoutString(token));
        return usersDao.isExistsUsernameTokenPair(username, md5Token);
    }

    private byte[] convertCharArrayToByteArrayWithoutString(@Nonnull char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        return byteBuffer.array();
    }
}

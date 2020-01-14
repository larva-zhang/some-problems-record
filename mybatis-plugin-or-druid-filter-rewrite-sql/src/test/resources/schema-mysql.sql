drop table if exists test_user;
CREATE TABLE `test_user`
(
    `id`          int(11)     NOT NULL AUTO_INCREMENT,
    `account`     varchar(70) NOT NULL COMMENT '账号',
    `user_name`   varchar(60) NOT NULL COMMENT '姓名',
    `age`         int(11)     NOT NULL COMMENT '年龄',
    `sex`         bit(1)      NOT NULL COMMENT '性别：0-男，1-女',
    `create_time` timestamp   NOT NULL DEFAULT '2019-01-01 00:00:00' COMMENT '创建时间',
    `update_time` timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account` (`account`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='用户信息表';
drop schema if exists `migrate_spring_boot_demo`;
create schema `migrate_spring_boot_demo`;
use `migrate_spring_boot_demo`;
create table `users`
(
  `id`         bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`   varchar(10) NOT NULL comment '用户名',
  `token`      varchar(32) NOT NULL COMMENT '密码',
  `created_at` timestamp   NOT NULL DEFAULT '2018-12-01 00:00:00',
  `updated_at` timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

insert into users(username, token, created_at, updated_at) values ('test', md5('test'), now(), now());
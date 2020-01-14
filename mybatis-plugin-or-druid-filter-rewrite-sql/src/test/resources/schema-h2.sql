drop table if exists test_user;
create table test_user
(
    id          int auto_increment
        primary key,
    account     varchar(70)                             not null comment '账号',
    user_name   varchar(60)                             not null comment '姓名',
    age         int                                     not null comment '年龄',
    sex         bit                                     not null comment '性别：0-男，1-女',
    create_time timestamp default '2019-01-01 00:00:00' not null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP     not null comment '更新时间',
    constraint uk_account
        unique (account)
);
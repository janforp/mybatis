DROP TABLE mt_employee;

CREATE TABLE mt_employee
(
    id    int auto_increment comment 'id,自增',
    name  varchar(100) comment '姓名',
    id_no char(20) comment '身份证号码'
);

INSERT INTO mt_employee (id, name, id_no)
VALUES (1, '张三', '4311101');

INSERT INTO mt_employee (id, name, id_no)
VALUES (2, '李四', '4311102');

INSERT INTO mt_employee (id, name, id_no)
VALUES (3, '王五', '4311103');

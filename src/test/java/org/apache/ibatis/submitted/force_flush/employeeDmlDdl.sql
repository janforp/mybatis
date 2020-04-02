DROP TABLE employee;

CREATE TABLE employee
(
    id   int comment 'id,自增',
    name varchar(100) comment '姓名',
    idNo char(20) comment '身份证号码'
);

INSERT INTO employee (id, name, idNo)
VALUES (1, '张三', '4311101');

INSERT INTO employee (id, name, idNo)
VALUES (2, '李四', '4311102');

INSERT INTO employee (id, name, idNo)
VALUES (3, '王五', '4311103');

-- 示例业务库（H2 内存，MySQL 兼容模式）——用于链路冒烟，不进入仓库
CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '商品名称',
  price DECIMAL(10, 2) NOT NULL COMMENT '商品单价',
  stock INT NOT NULL COMMENT '库存数量',
  category VARCHAR(50) COMMENT '商品分类',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间'
) COMMENT '商品表';

CREATE TABLE customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL COMMENT '客户姓名',
  phone VARCHAR(20) COMMENT '联系电话',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间'
) COMMENT '客户表';

CREATE TABLE sales_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL COMMENT '订单编号',
  customer_id BIGINT NOT NULL COMMENT '下单客户ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  quantity INT NOT NULL COMMENT '购买数量',
  total_amount DECIMAL(12, 2) NOT NULL COMMENT '订单金额',
  status VARCHAR(20) NOT NULL COMMENT '订单状态: paid/shipped/completed/cancelled',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
  CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES product (id)
) COMMENT '销售订单表';
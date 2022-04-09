package com.decucin.order.mapper;

import com.decucin.order.pojo.Order;
import org.apache.ibatis.annotations.Select;

public interface OrderMapper {

    @Select("select * from tb_order where id = #{id}")
    Order findById(Long id);
}

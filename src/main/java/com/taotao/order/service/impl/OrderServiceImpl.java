package com.taotao.order.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.druid.util.StringUtils;
import com.taotao.common.pojo.TaotaoResult;
import com.taotao.mapper.TbOrderItemMapper;
import com.taotao.mapper.TbOrderMapper;
import com.taotao.mapper.TbOrderShippingMapper;
import com.taotao.order.dao.JedisClient;
import com.taotao.order.service.OrderService;
import com.taotao.pojo.TbOrder;
import com.taotao.pojo.TbOrderItem;
import com.taotao.pojo.TbOrderShipping;
/**
 * 订单Service
 * @author ghs
 *
 */
@Service
public class OrderServiceImpl implements OrderService{

	@Autowired
	private TbOrderMapper orderMapper;
	
	@Autowired
	private TbOrderItemMapper itemMapper;
	
	@Autowired
	private TbOrderShippingMapper shippingMapper;
	
	@Autowired
	private JedisClient jedisClient;
	
	@Value("${ORDER_GEN_KEY}")
	private String ORDER_GEN_KEY;
	
	@Value("${ORDER_INIT_ID}")
	private String ORDER_INIT_ID;
	
	@Value("${ORDER_DETAIL_GEN_KEY}")
	private String ORDER_DETAIL_GEN_KEY;
	
	//插入订单信息到订单的相关表中
	@Override
	public TaotaoResult createOreder(TbOrder order, List<TbOrderItem> itemList, 
			TbOrderShipping orderShipping) {
		// 获取订单号
		String string = jedisClient.get(ORDER_GEN_KEY);
		if (StringUtils.isEmpty(string)) {
			jedisClient.set(ORDER_GEN_KEY, ORDER_INIT_ID);		//如果为空，设置初始值
		}
		long orderId = jedisClient.incr(ORDER_GEN_KEY);		//订单自增
		//补全pojo属性
		order.setOrderId(orderId + "");
		//状态：1、未付款，2、已付款，3、未发货，4、已发货，5、交易成功，6、交易关闭
		order.setStatus(1);
		Date date = new Date();
		order.setCreateTime(date);
		order.setUpdateTime(date);
		//0：未评价 1：已评价
		order.setBuyerRate(0);
		orderMapper.insert(order);
		
		//插入订单明细
		for (TbOrderItem orderItem : itemList) {
			//取订单明细id
			long orderDetailId = jedisClient.incr(ORDER_DETAIL_GEN_KEY);
			orderItem.setId(orderDetailId + "");
			orderItem.setOrderId(orderId + "");
			itemMapper.insert(orderItem);
		}
		//插入物流表
		orderShipping.setOrderId(orderId + "");
		orderShipping.setCreated(date);
		orderShipping.setUpdated(date);
		shippingMapper.insert(orderShipping);
		return TaotaoResult.ok(orderId);
	}

}

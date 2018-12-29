package com.springcloud.eshop.datasync.server;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DatasyncServerApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void json() {
//		//创建测试object
//		User user = new User("李宁",24,"北京");
//		System.out.println("======================" + user);
//
//		//转成json字符串
//		String json = JSON.toJSON(user).toString();
//		System.out.println("======================" + json);
//
		//json字符串转成对象
		String str = "{\"createBy\":\"刘佳\",\"productSpecification\":{\"productId\":\"89717875815747584\",\"name\":\"iphone 规格\",\"id\":\"90717046513340416\",\"delFlag\":0,\"value\":\"上市时间=2018-12-27 22:08:48;机身厚度=0.7cm;\"},\"createTime\":\"2018-12-27 22:19:25\",\"updateBy\":\"刘佳\",\"brandId\":\"90712083275976704\",\"name\":\"三星盖世8\",\"description\":\"三星手机\",\"productProperty\":{\"productId\":\"89717875815747584\",\"name\":\"iphone 属性\",\"id\":\"90715944527073280\",\"delFlag\":0,\"value\":\"机身颜色=金色,银色;存储内存=32G,128G\"},\"updateTime\":\"2018-12-27 22:19:32\",\"id\":\"89717875815747584\",\"delFlag\":0,\"categoryId\":\"90713253973331968\"}";
		JSONObject jsonObject = JSONUtil.parseObj(str);
		System.out.println("======================" + jsonObject.toString());

		List<String> list = new ArrayList<>();
		list.add("2");
		list.add("1");
//		list.stream().reduce((a, b) -> a + "," + b).ifPresent(System.out::println);

		StringBuilder builder = new StringBuilder();
		list.stream().forEach(i -> builder.append(i));
		System.out.println(builder.toString());
	}

}


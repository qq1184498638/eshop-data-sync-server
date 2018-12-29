package com.springcloud.eshop.datasync.server.service.feign;

import com.springcloud.eshop.common.server.support.utils.ServerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "product-server")
public interface ProductService {
    @GetMapping("/brand/findById")
    ServerResponse findByBrandId(@RequestParam("id") String id);

    @GetMapping("/brand/findByIds")
    ServerResponse findByBrandIds(@RequestParam("ids") String[] ids);

    @GetMapping("/category/get/{id}")
    ServerResponse findByCategoryId(@PathVariable("id") String id);

    @GetMapping("/product-intro/get/{id}")
    ServerResponse findByProductIntroId(@PathVariable("id") String id);

    @GetMapping("/product-property/get/{id}")
    ServerResponse findByProductPropertyId(@PathVariable("id") String id);

    @GetMapping("/product-specification/get/{id}")
    ServerResponse findByProductSpecificationId(@PathVariable("id") String id);

    @GetMapping("/product/findById")
    ServerResponse findByProductId(@RequestParam("id") String id);


}

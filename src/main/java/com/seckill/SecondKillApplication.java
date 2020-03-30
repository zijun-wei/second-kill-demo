package com.seckill;

import com.seckill.dao.UserDOMapper;
import com.seckill.dos.UserDO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @Title JodaDateTimeJsonDeserializer
 * @Description DateTime反序列化
 * @Author Zijun_Wei
 * @Date 2020/1/22
 * @EnableAutoConfiguration 这个注解和SpringBootApplication功能几乎一样，都使用springboot自动配置托管这个类的启动*/
@RestController
@SpringBootApplication(scanBasePackages = {"com.seckill"})
@MapperScan("com.seckill.dao")
public class SecondKillApplication {
    @Autowired
    private UserDOMapper userDOMapper;
    @RequestMapping("/")
    public String home(){
        UserDO userDO = userDOMapper.selectByPrimaryKey(1);
        if(userDO == null){
            return "用户不存在";
        }else{
            return userDO.getName();
        }
    }
    public static void main( String[] args ) {
        SpringApplication.run(SecondKillApplication.class, args);
    }
}

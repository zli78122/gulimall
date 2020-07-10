package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import com.atguigu.gulimall.thirdparty.component.SMSComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Autowired
    OSSClient ossClient;

    @Autowired
    SMSComponent smsComponent;

    @Test
    void testSendSms() {
        smsComponent.sendSMSCode("18931007018", "123456");
    }

    @Test
    void testOSSUpdate() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("/Users/zhengyuli/Desktop/avatar.png");
        ossClient.putObject("zli78122-gulimall", "avatar.png", inputStream);
        ossClient.shutdown();
    }
}

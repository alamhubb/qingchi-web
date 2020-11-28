package com.qingchi.admin;

import com.qingchi.base.repository.tag.TagTypeRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * @author qinkaiyuan
 * @date 2019-05-25 10:59
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class)
public class RedisTest {
    @Resource
    private TagTypeRepository tagTypeRepository;

    @Test
    public void testJpa() {
    }
}

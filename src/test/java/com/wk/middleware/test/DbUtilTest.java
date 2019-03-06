package com.wk.middleware.test;

import com.wk.middleware.annotations.Table;
import com.wk.middleware.entities.UuidEntity;
import com.wk.middleware.util.DBUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by jince on 2019/3/5.
 */
public class DbUtilTest {

    @Test
    public void t() {

        C c = new C();

        c.setName("c_name");
        c.setDes("c_des");

        System.out.println(c);

        JdbcTemplate jdbcTemplate = j();

        int i = DBUtil.insertOrUpdate(jdbcTemplate, c);

        System.out.println(i);
    }

    private JdbcTemplate j() {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://192.168.57.67:3306/fast_middle?characterEncoding=utf-8&useSSL=false&useUnicode=true");
        dataSource.setUsername("fast");
        dataSource.setPassword("1qaz@WSX");

        return new JdbcTemplate(dataSource);
    }

    @Table(name = "test_c")
    static class C extends UuidEntity {

        private String name;
        private String des;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDes() {
            return des;
        }

        public void setDes(String des) {
            this.des = des;
        }

        @Override
        public String toString() {

            return super.toString();
        }
    }
}

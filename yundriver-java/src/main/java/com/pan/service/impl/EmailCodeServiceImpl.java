package com.pan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pan.component.RedisComponent;
import com.pan.mapper.UserInfoMapper;
import com.pan.pojo.dto.SystemDto;
import com.pan.pojo.entity.EmailCode;
import com.pan.pojo.entity.UserInfo;
import com.pan.pojo.properties.MessageConfig;
import com.pan.service.EmailCodeService;
import com.pan.mapper.EmailCodeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.UUID;

/**
* @author 31696
* @description 针对表【email_code(邮箱验证码)】的数据库操作Service实现
* @createDate 2024-12-31 18:00:28
*/
@Service
public class EmailCodeServiceImpl extends ServiceImpl<EmailCodeMapper, EmailCode>
    implements EmailCodeService{

    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Resource
    private EmailCodeMapper emailCodeMapper;
    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private JavaMailSender javaMailSender;
    @Resource
    private MessageConfig messageConfig;
    @Resource
    private RedisComponent redisComponent;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, Integer type) {
        // type=0时为注册,1是找回密码
        if(type==0){
            LambdaQueryWrapper<UserInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(UserInfo::getEmail,email);
            UserInfo userInfo = userInfoMapper.selectOne(lambdaQueryWrapper);
            if(null!=userInfo){
                throw new RuntimeException("邮箱已经存在");
            }
        }
        // 销毁上一次邮箱码
        emailCodeMapper.disableLast(email);
        // 生成验证码
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 5);
        // 发邮件
        sendEmailCode(email,code);
        EmailCode emailCode = new EmailCode();
        emailCode.setCode(code);
        emailCode.setEmail(email);
        emailCode.setStatus(0); // 0未使用
        emailCode.setCreateTime(new Date());
        emailCodeMapper.insert(emailCode);
    }

    @Override
    public void checkCode(String email, String emailCode) {
        LambdaQueryWrapper<EmailCode> query = new LambdaQueryWrapper<EmailCode>().eq(EmailCode::getEmail, email).eq(EmailCode::getCode, emailCode);
        EmailCode ec = emailCodeMapper.selectOne(query);
        if(null == ec){
            throw new RuntimeException("验证码错误");
        }
        // 判断验证码是否过期
        if (ec.getStatus() == 1 || System.currentTimeMillis() - ec.getCreateTime().getTime()>15*1000*60){
            throw new RuntimeException("验证码过期");
        }
        emailCodeMapper.disableLast(email);
    }

    private void sendEmailCode(String dest,String code){
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true);
            helper.setFrom(messageConfig.getUsername());
            helper.setTo(dest);
            SystemDto systemDto = redisComponent.getSystemDto();
            helper.setSubject(systemDto.getTitle());
            helper.setText(String.format(systemDto.getEmailContent(),code));
            helper.setSentDate(new Date());
            javaMailSender.send(message);
        } catch (Exception e) {
            logger.error("邮件发送异常",e);
            throw new RuntimeException(e);
        }

    }
}





package com.pan.controller;

import com.pan.annotation.GlobalInterceptor;
import com.pan.annotation.VerifyParam;
import com.pan.component.RedisComponent;
import com.pan.constant.Constant;
import com.pan.exception.BussinessException;
import com.pan.pojo.dto.*;
import com.pan.pojo.enums.VerifyRegexEnum;
import com.pan.pojo.vo.Result;
import com.pan.service.EmailCodeService;
import com.pan.service.UserInfoService;
import com.pan.utils.CreateImageCode;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@RestController
public class AccountController {
    @Resource
    private UserInfoService userInfoService;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private RedisComponent redisComponent;

    @GetMapping("/checkCode")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void checkCode(HttpServletResponse response, HttpSession session, @VerifyParam(required = true) Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        // 0的话是邮箱验证码，1是登录或者注册的
        if (type == null || type == 0) {
            session.setAttribute(Constant.CHECK_CODE_EMAIL, code);
        } else {
            session.setAttribute(Constant.CHECK_CODE, code);
        }
        vCode.write(response.getOutputStream());
    }

    @PostMapping("/sendEmailCode")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public Result sendEmailCode(HttpSession session, @VerifyParam @RequestBody CodeDto dto) {
        try {
            if (!session.getAttribute(Constant.CHECK_CODE_EMAIL).equals(dto.getCheckCode())) {
                throw new BussinessException("验证码错误");
            }
            emailCodeService.sendEmailCode(dto.getEmail(), dto.getType());
            return Result.success("发送成功,请注意查收");
        } catch (NullPointerException e) {
            return Result.error("验证码已过期");
        } finally {
            session.removeAttribute(Constant.CHECK_CODE_EMAIL);
        }
    }

    @PostMapping("/register")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public Result register(HttpSession session, @VerifyParam @RequestBody RegisterDto registerDto) {
        try {
            if (!session.getAttribute(Constant.CHECK_CODE).equals(registerDto.getCheckCode())) {
                throw new BussinessException("验证码错误");
            }
            userInfoService.register(registerDto);
            return Result.success("注册成功");
        } finally {
            session.removeAttribute(Constant.CHECK_CODE);
        }
    }

    @PostMapping("/login")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public Result login(HttpSession session, @RequestBody LoginDto loginDto) {
        try {
            if (!session.getAttribute(Constant.CHECK_CODE).equals(loginDto.getCheckCode())) {
                throw new BussinessException("验证码错误");
            }
            SessionWebUserDto result = userInfoService.login(loginDto);
            session.setAttribute(Constant.SESSION_LOGIN_KEY, result);
            return Result.success(result);
        } finally {
            session.removeAttribute(Constant.CHECK_CODE);
        }
    }

    /*
    忘记密码
     */
    @PostMapping("/forgetPassword")
    @GlobalInterceptor(checkLogin = false,checkParams = true)
    public Result forgetPassword(HttpSession session,@RequestBody ResetDto dto) {
        if (!session.getAttribute(Constant.CHECK_CODE).equals(dto.getCheckCode())) {
            throw new BussinessException("验证码错误");
        }
        userInfoService.forgetPassword(dto);
        return Result.success("重置密码成功");
    }

    /*
    获取用户容量信息
     */
    @GetMapping("/getSpace")
    @GlobalInterceptor(checkLogin = true)
    public Result getSpaceMsg(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        UserSpaceDto userSpace = redisComponent.getUserSpace(sessionWebUserDto.getUserId());
        return Result.success(userSpace);
    }

    /*
    修改密码,已经登录的情况下
     */
    @PutMapping("/updatePassword")
    @GlobalInterceptor(checkParams = true)
    public Result updatePassword(HttpSession session, @RequestBody UpdatePasswordDto dto) {
        if (!session.getAttribute(Constant.CHECK_CODE).equals(dto.getCheckCode())) {
            throw new BussinessException("验证码错误");
        }
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        userInfoService.updatePassword(sessionWebUserDto.getUserId(),dto);
        return Result.success("修改成功");
    }

    @PostMapping("/logout")
    public Result logout(HttpSession session){
        session.invalidate();
        return Result.success("退出成功");
    }
}

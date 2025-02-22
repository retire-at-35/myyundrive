import time

from .base_page import BasePage
from selenium.webdriver.common.by import By
class SmokePage(BasePage):
    """
    登录页面的页面对象，包含登录页面的所有元素定位和操作方法
    """
    # 定义页面元素的定位器
    username_input = (By.XPATH, '//input[@placeholder="请输入账号"]')  # 账号输入框
    password_input = (By.XPATH, '//input[@placeholder="请输入密码"]')  # 密码输入框
    check_code_input = (By.XPATH,'//input[@placeholder="请输入验证码"]') # 登录按钮
    sumbit_btn = (By.XPATH,'//*[@id="app"]/div/div/div[1]/div/form/div[4]/div/button') # 提交按钮
    upload_btn = (By.XPATH, '/html/body/div[1]/div/div[2]/div[2]/div/div[1]/div[1]/button[1]') #上传文件
    # sel_btn = (By.XPATH, '/html/body/div[1]/div/div[2]/div[2]/div/div[4]/div/div/div/div/div/div/div/button')
    upload_input = (By.XPATH, '//input[@type="file"]')
    start_upload_btn = (By.XPATH,'/html/body/div[1]/div/div[2]/div[2]/div/div[4]/div/div/div/div/div/button')
    success_msg =(By.XPATH,'//*[@id="message_2"]/p')
    # 登录方法
    def login(self, username, password,verification_code):
        """
        执行登录操作
        :param username: 用户名
        :param password: 密码
        """
        self.input_text(self.username_input, username)  # 输入用户名
        self.input_text(self.password_input, password)  # 输入密码
        self.input_text(self.check_code_input,verification_code)  # 输入验证码
        self.click(self.sumbit_btn)
        time.sleep(3)
        assert self.driver.current_url == 'http://114.132.59.198:18080/home/index'
    # 上传方法
    def upload(self, param):
        self.click(self.upload_btn)
        self.upload_file(self.upload_input,'D:\code\yundriver_pytest\check_code\captcha.png')
        time.sleep(3)
        # 点击上传文件的按钮
        self.click(self.start_upload_btn)
        # 断言是否上传成功
        assert '所有文件上传成功' == self.get_text(self.success_msg)


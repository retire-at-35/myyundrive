import time

from selenium.webdriver.common.by import By
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from page_object.smoke_page import SmokePage
from utils.logger import logger
from utils.chaojiying import Chaojiying_Client

class TestLogin:




    def test_smoke(self, driver):
        """
        测试登录成功的场景
        :param driver: WebDriver实例
        """
        
        # 登录操作
        smoke_page = SmokePage(driver)
        wait = WebDriverWait(driver, 10)
        time.sleep(2)
        captcha_img = wait.until(EC.presence_of_element_located(
            (By.XPATH, '//*[@id="app"]/div/div/div[1]/div/form/div[3]/div/div/div[2]/img')))
        captcha_img.screenshot('../check_code/captcha.png')
        verification_code = self.get_verification_code('../check_code/captcha.png')
        smoke_page.login('3169642314@qq.com','123456',verification_code)

        # 上传文件
        smoke_page.upload('../check_code/captcha.png')



    def get_verification_code(self,image_path):
        # 初始化超级鹰客户端
        chaojiying = Chaojiying_Client()
        with open(image_path, 'rb') as img_file:
            img_data = img_file.read()
        # 调用超级鹰 API 识别验证码
        result = chaojiying.PostPic(img_data,1902)
        if result['err_no'] == 0:
            return result['pic_str']
        else:
            print(f"验证码识别失败，错误信息: {result['err_str']}")
            return None
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from utils.logger import logger

class BasePage:
    """
    页面基类，包含了所有页面都会用到的常用方法
    """
    def __init__(self, driver):
        """
        初始化基类
        :param driver: WebDriver实例
        """
        self.driver = driver
        # 创建一个WebDriverWait对象，用于显式等待
        self.wait = WebDriverWait(self.driver, 20)


    def find_element(self, locator, timeout=10):
        """
        查找单个元素的方法
        :param locator: 元素定位器，格式为(By.XX, "value")，如(By.ID, "username")
        :param timeout: 超时时间，默认10秒
        :return: 返回找到的元素
        """
        try:
            # 使用显式等待查找元素，直到元素出现
            element = self.wait.until(
                EC.presence_of_element_located(locator)
            )
            logger.info(f"找到元素: {locator}")
            return element
        except Exception as e:
            logger.error(f"未找到元素 {locator}, 错误信息: {str(e)}")
            raise

    def click(self, locator):
        """
        点击元素的方法
        :param locator: 元素定位器
        """
        element = self.find_element(locator)
        element.click()
        logger.info(f"点击元素: {locator}")

    def input_text(self, locator, text):
        """
        在输入框中输入文本
        :param locator: 元素定位器
        :param text: 要输入的文本
        """
        element = self.find_element(locator)
        element.clear()  # 先清空输入框
        element.send_keys(text)  # 输入文本
        logger.info(f"在元素 {locator} 中输入文本: {text}")

    def get_text(self, locator):
        """
        获取元素的文本内容
        :param locator: 元素定位器
        :return: 元素的文本内容
        """
        element = self.find_element(locator)
        return element.text

    def upload_file(self, locator, file_path):
        """
        上传文件的方法
        :param locator: 元素定位器，用于定位文件上传输入框
        :param file_path: 要上传的文件的绝对路径
        """
        try:
            file_input = self.find_element(locator)
            file_input.send_keys(file_path)
            logger.info(f"成功上传文件 {file_path} 到元素 {locator}")
        except Exception as e:
            logger.error(f"文件上传失败，元素定位器: {locator}, 文件路径: {file_path}, 错误信息: {str(e)}")
            raise
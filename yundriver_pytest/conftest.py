import pytest
from selenium import webdriver


@pytest.fixture(scope="function")
def driver():
    driver = webdriver.Edge()
    driver.maximize_window()
    driver.get("http://114.132.59.198:18080/")
    yield driver
    driver.quit()
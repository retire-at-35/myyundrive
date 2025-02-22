import logging
import os
from datetime import datetime

# 创建logs目录（如果不存在）
if not os.path.exists("logs"):
    os.mkdir("logs")

# 设置日志格式：时间 - 日志级别 - 日志信息
log_format = '%(asctime)s - %(levelname)s - %(message)s'
date_format = '%Y-%m-%d %H:%M:%S'

# 创建logger实例
logger = logging.getLogger('web_automation')
logger.setLevel(logging.INFO)  # 设置日志级别为INFO

# 创建文件处理器，用于将日志写入文件
# 文件名格式：test_年月日_时分秒.log
file_handler = logging.FileHandler(
    f"logs/test_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log",
    encoding='utf-8'
)
file_handler.setFormatter(logging.Formatter(log_format, date_format))

# 创建控制台处理器，用于在控制台显示日志
console_handler = logging.StreamHandler()
console_handler.setFormatter(logging.Formatter(log_format, date_format))

# 将处理器添加到logger
logger.addHandler(file_handler)
logger.addHandler(console_handler) 
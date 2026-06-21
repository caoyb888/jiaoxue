#!/usr/bin/env python3
"""
生成 1000 名压测学生账号的 CSV 文件。
格式：studentId,username,password
密码统一使用测试默认密码（edu2026@test）。

用法：
    python3 gen-students-csv.py > students-1000.csv

输出的账号须与 seed_dev.sql 中批量插入的测试学生数据一致。
"""
import sys

COUNT = 1000
DEFAULT_PASSWORD = "edu2026@test"
START_ID = 10001

print("studentId,username,password")
for i in range(COUNT):
    student_id = START_ID + i
    # 学号末两位取模：student_id % 100 → 0~99，对应 getSubmitDelay 的输入
    username = f"student{str(i + 1).zfill(5)}"
    print(f"{student_id},{username},{DEFAULT_PASSWORD}")

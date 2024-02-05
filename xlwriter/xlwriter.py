#!/usr/bin/env python3
# -*- coding: utf8 -*-

import os
import sys
import yaml
import subprocess
import re
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from email.mime.base import MIMEBase
from email import encoders
from datetime import datetime
from string import ascii_lowercase


def module_install(_module_name):
    r = subprocess.run(
        "pip3 list".split(), 
        stdout=subprocess.PIPE).stdout.decode().lower()
    if not re.search(_module_name.lower(), r):
        subprocess.run([f'pip3 install {_module_name}'], shell=True)



def send_email(sender_email, receiver_email, subject, body, attachment_path):
    # Set up the MIME object
    message = MIMEMultipart()
    message["From"] = sender_email
    message["To"] = receiver_email
    message["Subject"] = subject

    # Attach the body of the email
    message.attach(MIMEText(body, "plain"))

    # Attach the Excel sheet
    attachment = open(attachment_path, "rb")
    part = MIMEBase("application", "octet-stream")
    part.set_payload(attachment.read())
    encoders.encode_base64(part)
    part.add_header("Content-Disposition", f"attachment; filename= {attachment_path}")
    message.attach(part)

    # Connect to the SMTP server and send the email
    smtp_server = "localhost"
    smtp_port = 25
    smtp_username = ""
    smtp_password = ""

    with smtplib.SMTP(smtp_server, smtp_port) as server:
        server.starttls()
        if smtp_username != "":
            server.login(smtp_username, smtp_password)
        server.sendmail(sender_email, receiver_email, message.as_string())


def generate_yaml_template(_yaml_file):
    
    DEFAULT_COLUMN_SIZE = 30
    ATTRIBUTES = [
        'cell_value', 
        'cell_merge', 
        'cell_bg', 
        'cell_border', 
        'font_name: Arial', 
        'font_size', 
        'font_bold: False', 
        'font_color',
        'font_h_align: center', 
        'font_v_align: vcenter'
        ]
    print("Specify how many rows and columns to write:")
    while True:
        num_rows = input("Rows: ")
        try:
            num_rows = int(num_rows)
            break
        except:
            print("[ERROR] input must be an integer!")
    while True:
        num_columns = input("Columns: ")
        try:
            num_columns = int(num_columns)
            break
        except:
            print("[ERROR] input must be an integer!")
    with open(_yaml_file, 'w', encoding='utf-8') as f:
        f.write("# Enter values as parameters by preceed with '$' sign. e.g: 'cell_bg: $BG_VARIABLE'\n")
        f.write("# Colors:\n")
        f.write("#   Custom colors are set in COLOR dictionary e.g. $COLOR['light_gray']\n")
        f.write("#   Colors can be one of:\n")
        f.write("#   - HTML style #RRGGBB string\n")
        f.write("#   - Named colors as listed in https://xlsxwriter.readthedocs.io/working_with_colors.html#colors\n")
        f.write("# Attributes:\n")
        f.write("#   cell_merge: <range> (e.g: A4:D5)\n")
        f.write("#   border: <integer> (e.g: 1 | 2 | 5 ...)\n")
        f.write("#   bold: True | False (default)\n")
        f.write("#   font_h_align: left | center (default)| right | fill | justify | center_across | distributed\n")
        f.write("#   font_v_align: top | vcenter (default) | bottom | vjustify | vdistributed\n")
        for c in range(0, num_columns):
            f.write(f"{ascii_lowercase[c].upper()}:\n")
            f.write(f"  width: {DEFAULT_COLUMN_SIZE}\n")
            f.write(f"  rows:\n")
            for r in range(1, num_rows + 1):
                f.write(f"    {r}:\n")
                for k in ATTRIBUTES:
                    if ':' in k:
                        f.write(f"      {k}\n")
                        continue
                    f.write(f"      {k}: \n")


if __name__ == '__main__':
    WORKBOOK = 'spreadsheet.xlsx'
    sender = "daily-reports@tau.ac.il"
    receiver = "alon.segal@gigaspaces.com"
    #receiver = "alon.segal@gigaspaces.com, josh.roden@gigaspaces.com, ami.zvieli@gigaspaces.com, aharon.moll@gigaspaces.com"
    subject = "Auto Generated Excel Report Example"
    body = "Please find the daily report in the attached Excel sheet."
    attachment = WORKBOOK
    COLOR = {
        'black': '#000000',
        'white': '#FFFFFF',
        'yellow': '#FFFF00',
        'red': '#FF0000',
        'dark_gray': '#333333', 
        'light_gray': '#808080', 
        'light_blue': '#0066CC'
        }
    
    # list of required python modules 
    modules = ['xlsxwriter']
    
    # install modules
    for mod in modules:
        module_install(mod)

    # import modules
    import xlsxwriter

    # get yaml file as parameter and generate if not exists
    if len(sys.argv) < 2:
        print("[ERROR] missing parameter for yaml file.")
        exit(1)
    else:
        yaml_file = os.path.abspath(sys.argv[1])
    if not os.path.exists(yaml_file):
        print(f"[INFO] {yaml_file} does not exist. create it? ")
        while True:
            ans = input("y/n? ")
            if ans.lower() == 'y':
                generate_yaml_template(yaml_file)
                break
            if ans.lower() == 'n':
                exit()
            else:
                print("enter 'y' or 'n'")
        print(f"[INFO] yaml file '{yaml_file}' created successfully")
        print("[INFO] Fill with values and re-run to generate excel file.\n")
        exit()
    # load yaml data
    with open(yaml_file, 'r') as yf:
        data = yaml.safe_load(yf)

    # Get the current date as "dd/mm/yyyy"
    today = datetime.now().strftime("%d/%m/%Y")

    # which is the filename that we want to create.
    workbook = xlsxwriter.Workbook(WORKBOOK)
    
    # create a format object
    cell_format = workbook.add_format()

    # The workbook object is then used to add new 
    # worksheet via the add_worksheet() method.
    worksheet = workbook.add_worksheet()
    

    # Set the sheet to RTL
    worksheet.right_to_left()
    
    for col in data.keys():
        col_width = data[col]['width']
        for row in data[col]['rows']:
            format_properties = {}
            cell_value = data[col]['rows'][row]['cell_value']
            cell_merge = data[col]['rows'][row]['cell_merge']
            cell_bg = data[col]['rows'][row]['cell_bg']
            cell_border = data[col]['rows'][row]['cell_border']
            font_name = data[col]['rows'][row]['font_name']
            font_size = data[col]['rows'][row]['font_size']
            font_bold = data[col]['rows'][row]['font_bold']
            font_color = data[col]['rows'][row]['font_color']
            font_h_align = data[col]['rows'][row]['font_h_align']
            font_v_align = data[col]['rows'][row]['font_v_align']
        
            # set font color
            if font_color is not None:
                if font_color.startswith("$"):
                    font_color = eval(font_color.strip('$'))
                format_properties['font_color'] = font_color

            # set font size
            if font_size is not None:
                format_properties['font_size'] = int(font_size)

            # set font name
            if font_name is not None:
                format_properties['font_name'] = font_name
            
            # set font bold
            if font_bold:
                format_properties['bold'] = True

            # set cell background
            if cell_bg is not None:
                if cell_bg.startswith("$"):
                    cell_bg = eval(cell_bg.strip('$'))
                format_properties['bg_color'] = cell_bg
            
            # set font alignment
            if font_h_align is not None:
                format_properties['align'] = font_h_align
            if font_v_align is not None:
                format_properties['valign'] = font_v_align
            
            # set border
            if cell_border is not None:
                format_properties['border'] = int(cell_border)

            # set cell value
            if cell_value is not None:
                if cell_value.startswith("$"):
                    cell_value = eval(cell_value.strip('$'))
            
            # write regular or merged range 
            if cell_merge is not None:
                worksheet.merge_range(cell_merge, cell_value, workbook.add_format(format_properties))
            else:
                worksheet.write(f"{col}{row}", cell_value, workbook.add_format(format_properties))
        
        # set column width
        worksheet.set_column(f"{col}:{col}", col_width)

# close the Excel file
workbook.close()

print(f"[INFO] spreadsheet saved as '{WORKBOOK}'\n")


# send email
send_email(sender, receiver, subject, body, attachment)
print(f"[INFO] email with {WORKBOOK} was sent.\n")
exit()

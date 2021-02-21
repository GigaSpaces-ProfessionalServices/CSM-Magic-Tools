CSM tool for executing commands on all nodes in the cluster  
We are using the first node in the cluster for management.

1.  The following command will overright default id_rsa keys of the user, use -f key in order to specify a non default filenames.
create ssh key:

    `cd ~`
    `ssh-keygen -t rsa -f .ssh/gs_ods` 
    
    Hit `enter` in response for all questions.
    
    2 Options:
    1. `cp .ssh/gs_ods .ssh/id_rsa`
    2. Each command do:
       `ssh -i .ssh/gs_ods`

Append the content of .ssh/gs_ods.pub to .ssh/authorized_keys file on all nodes in the cluster  
or use  

    ssh-copy-id -i .ssh/gs_ods.pub user_name@PRIVATE_IP 
    
**Note:**

We need to use user's password in order to run the previous command.

On aws instances we need to manually set the password and allow ssh with password.   
Execute the following steps on each host:

a)      Append the following two lines to **/etc/ssh/sshd_config** file:
  
    Match User ec2-user 
    PasswordAuthentication yes  
b)      restart the ssh daemon:

    sudo systemctl restart sshd
       
c)    set password for ec2-user.

    sudo -i 
    passwd ec2-user 

2. create nodes.txt file and make a list of private IPs of all nodes   
    [ec2-user@ip-9-0-1-172 ~]$ **cat nodes.txt**  
    **9.0.1.172**    
    **9.0.1.119**   
    **9.0.1.84**     

3. execute a command on all nodes  
[ec2-user@ip-9-0-1-172 ~]$ **cat nodes.txt |xargs -i ssh {} date**  
    **Sun Nov  1 13:30:01 UTC 2020**  
    **Sun Nov  1 13:30:01 UTC 2020**  
    **Sun Nov  1 13:30:01 UTC 2020**  
[ec2-user@ip-9-0-1-172 ~]$  

4. make an alias to simplify the use:  
**alias csm='cat nodes.txt | xargs -i ssh {}'**  
[ec2-user@ip-9-0-1-172 ~]$ **csm date**   
**Sun Nov  1 13:31:08 UTC 2020**  
**Sun Nov  1 13:31:08 UTC 2020**  
**Sun Nov  1 13:31:08 UTC 2020**  
[ec2-user@ip-9-0-1-172 ~]$ 

**TIP:**
**Add to ~/.bash_profile**

5. Add the **csm** alias to **~/.bash_profile** file.
  
6. Prepare a folder for SW packages on all nodes:  
    [ec2-user@ip-9-0-1-172 ~]$ **csm "sudo mkdir /var/soft"**<br>
    [ec2-user@ip-9-0-1-172 ~]$ **csm "sudo chmod 777 /var/soft"**<br>
    [ec2-user@ip-9-0-1-172 ~]$ **csm "sudo chmod 777 /opt"**

Download java and GigaSpaces package to the management server, copy the packages to all nodes
and use the csm tool to install the packages:    

    cat nodes.txt | xargs -i scp -i .ssh/id_rsa jdk-11.0.10_linux-x64_bin.rpm  {}:/var/soft/
    csm "sudo rpm -ivh /var/soft/jdk*"

    cat nodes.txt | xargs -i scp -i .ssh/id_rsa gigaspaces-smart-ods-enterprise-15.8.0.zip  {}:/var/soft/      
    csm "cd /opt/; unzip /var/soft/gigaspaces-smart-ods-enterprise-15.8.0.zip > /dev/null"
    
   **Note:**<br>
    For aws instances install unzip before running the above command:
    
    [ec2-user@ip-9-0-1-172 ~]$ csm "sudo yum -y install unzip"
    
   If no internet connection do:
   
    cp /var/cache/yum/x86_64/7Server/rhui-REGION-rhel-server-releases/packages/unzip-6.0-21.el7.x86_64.rpm /tmp
    cat nodes.txt |xargs -i scp /tmp/unzip-6.0-21.el7.x86_64.rpm {}:/tmp
    csm “sudo yum install -y /tmp/unzip-6.0-21.el7.x86_64.rpm ”
    [ec2-user@ip-14-15-100-147 ~]$ csm ‘which unzip 
    /usr/bin/unzip
    /usr/bin/unzip
    ......
    

Take care of the **GS_LICENSE, GS_MANAGER_SERVERS etc.**<br>
Edit the **setenv-overrides.sh** file on the local server, then copy it to all nodes:

    cat nodes.txt | xargs -i scp -i .ssh/id_rsa setenv-overrides.sh  {}:/opt/gigaspaces-smart-ods-enterprise-15.8.0/bin/setenv-overrides.sh
    
If private IP needs to be set in **setenv-overrides.sh** use **set_env.sh** script.
     
    cat nodes.txt | xargs -i scp -i .ssh/id_rsa set_env.sh  {}:/tmp/
    csm "chmod +x /tmp/set_env.sh"
    csm "/tmp/set_env.sh /opt/gigaspaces-smart-ods-enterprise-15.8.0/bin/setenv-overrides.sh"

          
  Using systemd service management for starting and stopping GigaSpaces Grid' 
  
##### IMPORTANT   
Edit **start_gs.sh & stop_gs.sh** scripts and make sure the path to the **gs.sh** is correct.  
In our case the GigaSpaces packages was extracted in **/opt/** folder

Copy **start_gs.sh  stop_gs.sh** scripts to **/usr/local/bin** folder.  
Make both files executable      

        chmod +x start_gs.sh  
        chmod +x stop_gs.sh   

Copy **gs.service** to **/etc/systemd/system/** folder  


Use scp to copy the files to other nodes in the cluster  

    cat nodes.txt | xargs -i scp -i .ssh/id_rsa st*_gs.sh {}:/tmp 
    csm 'sudo mv /tmp/st*_gs.sh /usr/local/bin/'  

    cat nodes.txt | xargs -i scp -i .ssh/id_rsa gs.service {}:/tmp  
    csm sudo mv /tmp/gs.service /etc/systemd/system/  

start the service on all nodes:  

    [ec2-user@ip-9-0-1-172 ~]$ csm 'sudo systemctl daemon-reload'  
    [ec2-user@ip-9-0-1-172 ~]$ csm 'sudo systemctl start gs.service'
    [ec2-user@ip-9-0-1-172 ~]$ csm 'sudo systemctl enable gs.service'   
    [ec2-user@ip-9-0-1-172 ~]$ csm 'systemctl is-active gs.service'  
**active**  
**active**  
**active**  

To stop the service on all nodes:

    [ec2-user@ip-9-0-1-172 ~]$ csm 'sudo systemctl stop gs.service'


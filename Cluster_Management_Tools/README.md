CSM tool for executing commands on all nodes in the cluster  
We are using the first node in the cluster for management.

1.  The following command will overright default id_rsa keys of the user, use -f key in order to specify a non default filenames.
create ssh key:

    `ssh-keygen -t rsa` 

Append the content of .ssh/id_rsa.pub to .ssh/authorized_keys file on all nodes in the cluster  
or use  

    ssh-copy-id -i .ssh/id_rsa.pub user_name@PRIVATE_IP 
    
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
    [ec2-user@ip-9-0-1-172 ~]$ cat nodes.txt  
    9.0.1.172    
    9.0.1.119   
    9.0.1.84     

3. execute a command on all nodes  
[ec2-user@ip-9-0-1-172 ~]$ cat nodes.txt |xargs -i ssh {} date  
Sun Nov  1 13:30:01 UTC 2020  
Sun Nov  1 13:30:01 UTC 2020  
Sun Nov  1 13:30:01 UTC 2020  
[ec2-user@ip-9-0-1-172 ~]$  

4. make an alias to simplify the use:  
alias csm='cat nodes.txt|xargs -i ssh {}'  
[ec2-user@ip-9-0-1-172 ~]$ csm date   
Sun Nov  1 13:31:08 UTC 2020  
Sun Nov  1 13:31:08 UTC 2020  
Sun Nov  1 13:31:08 UTC 2020  
[ec2-user@ip-9-0-1-172 ~]$  
  
Prepare a folder for SW packages on all nodes:
    [ec2-user@ip-9-0-1-172 ~]$ csm "sudo mkdir /var/soft"

Download java and GigaSpaces package to the management server, copy the packages to all nodes
and use teh csm tool to install the packages    

    cat host_list.txt |xargs -i scp -i .ssh/id_rsa jdk-11.0.10_linux-x64_bin.rpm  {}:/var/soft/
    csm "sudo rpm -ivh /var/soft/jdk*"

    cat host_list.txt |xargs -i scp -i .ssh/id_rsa gigaspaces-smart-ods-enterprise-15.8.0.zip  {}:/var/soft/      
    csm "cd /opt/; unzip /var/soft/gigaspaces-smart-ods-enterprise-15.8.0.zip > /dev/null "

Take care of the GS license. First edit the license file on the local server, then copy it to all nodes:

    cat host_list.txt |xargs -i scp -i .ssh/id_rsa /opt/gigaspaces-smart-ods-enterprise-15.8.0/gs-license.txt  {}:/opt/gigaspaces-smart-ods-enterprise-15.8.0/gs-license.txt
    
If private IP needs to be set in setenv-overrides.sh use set_env.sh script.
     
    cat host_list.txt |xargs -i scp -i .ssh/id_rsa set_env.sh  {}:/tmp/
    csm "/tmp/set_env.sh /opt/gigaspaces-smart-ods-enterprise-15.8.0/bin/setenv-overrides.sh"

          
  Using systemd serivce management for starting and stoppig GigaSpaces Grid' 
  
IMPORTANT   
Edit the following scripts and make sure the path to the gs.sh is correct.  
In our case the GigaSpaces packages was extracted in /opt/ folder  
First we install and configure the software and the scripts on the management server and tne copy them to the rest of the hosts

Copy start_gs.sh  stop_gs.sh scripts to /usr/local/bin folder.  
Make both files executable      

        chmode +x start_gs.sh  
        chmdode +x stop_gs.sh   

**Copy gs.service to /etc/systemd/system/ folder**   


Use scp to copy the files to other nodes in the cluster  

    cat nodes |xargs -i cp /usr/local/bin/st*_gs.sh {}:/tmp  
    csm 'sudo mv /tmp/st*_gs.sh /usr/local/bin/'  

    cat nodes |xargs -i cp /etc/systemd/system/gs.service {}:/tmp  
    csm sudo mv /tmp/gs.service /etc/systemd/system/  

start the service on all nodes:  

    [ec2-user@ip-9-0-1-172 ~]$ csm 'sudo systemctl daemon-reload '  
    [ec2-user@ip-9-0-1-172 ~]$ csm 'sudo systemctl start gs.service '   
    [ec2-user@ip-9-0-1-172 ~]$ csm 'systemctl is-active gs.service '  
active  
active  
active  

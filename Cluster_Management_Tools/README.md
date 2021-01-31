CSM tool for executing commands on all nodes in the cluster  
We are using the first node in the cluster for management.

1.  The following command will overright default id_rsa keys of the user, use -f key in order to specify a non default filenames.
create ssh key:

    `ssh-keygen -t rsa` 

Append the content of .ssh/id_rsa.pub to .ssh/authorized_keys file on all nodes in the cluster  
or use  

    ssh-copy-id -i .ssh/id_rsa.pub user_name@PRIVATE_IP 
    
**Note:**

We need to use user's passeword in order to run the previous command.
On aws instances we need to manually set the password and allow ssh with password.   
Execute the following steps on each host:   
a)    
Append the following two lines to /etc/ssh/sshd_config  file:   
Match User ec2-user 
PasswordAuthentication yes  
b)  
restart the ssh daemon: 
sudo systemctl restart sshd     
c)    
set password for ec2-user.  
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
  
    
      

Using systemd serivce management for starting and stoppig GigaSpaces Grid 
  
IMPORTANT   
Edit the following scripts and make sure the path to the gs.sh is correct.  
In our case the GigaSpaces packages was extracted in /opt/ folder  

Copy start_gs.sh  stop_gs.sh scripts to /usr/local/bin folder.  
Make both files executable  
chmode +x start_gs.sh  
chmdode +x stop_gs.sh   
Copy gs.service to /etc/systemd/system/ folder   


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

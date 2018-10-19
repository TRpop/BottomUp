package com.idiots.bottomup;

import android.os.AsyncTask;
import android.util.Log;

import com.jcraft.jsch.*;

import java.io.File;

public abstract class SFTP {

    private static String TAG = "SFTP";

    private JSch jsch;
    private Session session;
    private ChannelSftp c;

    private String user, passwd, host;
    private int port;

    public SFTP(String user, String passwd, String host, int port){
        this.user = user;
        this.host = host;
        this.port = port;
        this.passwd = passwd;

        jsch=new JSch();
    }

    public void upload(final File file){
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    session=jsch.getSession(user, host, port);

                    MyUserInfo ui=new MyUserInfo();
                    ui.setPasswd(passwd);
                    session.setUserInfo(ui);

                    session.connect();

                    Channel channel=session.openChannel("sftp");
                    channel.connect();
                    c=(ChannelSftp)channel;
                    return true;
                } catch (JSchException e) {
                    Log.d(TAG, e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean tf) {
                super.onPostExecute(tf);
                if(tf) put(file);
            }
        }.execute();
    }

    public void disconnect(){
        c.quit();
        session.disconnect();
    }

    public void put(final File src){
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    int mode = ChannelSftp.OVERWRITE;
                    c.put(src.getAbsolutePath(), ".", mode);
                    return true;
                } catch (SftpException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean tf) {
                super.onPostExecute(tf);
                onUploadEnded(tf);
                disconnect();
            }
        }.execute();
    }

    abstract protected void onUploadEnded(Boolean tf);

    private class MyUserInfo implements UserInfo{

        private String passwd = null;

        public void setPasswd(String passwd){
            this.passwd = passwd;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public String getPassword() {
            return passwd;
        }

        @Override
        public boolean promptPassword(String s) {
            return passwd != null;
        }

        @Override
        public boolean promptPassphrase(String s) {
            return true;
        }

        @Override
        public boolean promptYesNo(String s) {
            return true;
        }

        @Override
        public void showMessage(String s) {
            Log.d(TAG, s);
        }
    }

    private static String help =
            "      Available commands:\n"+
                    "      * means unimplemented command.\n"+
                    "cd path                       Change remote directory to 'path'\n"+
                    "lcd path                      Change local directory to 'path'\n"+
                    "chgrp grp path                Change group of file 'path' to 'grp'\n"+
                    "chmod mode path               Change permissions of file 'path' to 'mode'\n"+
                    "chown own path                Change owner of file 'path' to 'own'\n"+
                    "df [path]                     Display statistics for current directory or\n"+
                    "                              filesystem containing 'path'\n"+
                    "help                          Display this help text\n"+
                    "get remote-path [local-path]  Download file\n"+
                    "get-resume remote-path [local-path]  Resume to download file.\n"+
                    "get-append remote-path [local-path]  Append remote file to local file\n"+
                    "hardlink oldpath newpath      Hardlink remote file\n"+
                    "*lls [ls-options [path]]      Display local directory listing\n"+
                    "ln oldpath newpath            Symlink remote file\n"+
                    "*lmkdir path                  Create local directory\n"+
                    "lpwd                          Print local working directory\n"+
                    "ls [path]                     Display remote directory listing\n"+
                    "*lumask umask                 Set local umask to 'umask'\n"+
                    "mkdir path                    Create remote directory\n"+
                    "put local-path [remote-path]  Upload file\n"+
                    "put-resume local-path [remote-path]  Resume to upload file\n"+
                    "put-append local-path [remote-path]  Append local file to remote file.\n"+
                    "pwd                           Display remote working directory\n"+
                    "stat path                     Display info about path\n"+
                    "exit                          Quit sftp\n"+
                    "quit                          Quit sftp\n"+
                    "rename oldpath newpath        Rename remote file\n"+
                    "rmdir path                    Remove remote directory\n"+
                    "rm path                       Delete remote file\n"+
                    "symlink oldpath newpath       Symlink remote file\n"+
                    "readlink path                 Check the target of a symbolic link\n"+
                    "realpath path                 Canonicalize the path\n"+
                    "rekey                         Key re-exchanging\n"+
                    "compression level             Packet compression will be enabled\n"+
                    "version                       Show SFTP version\n"+
                    "?                             Synonym for help";

}

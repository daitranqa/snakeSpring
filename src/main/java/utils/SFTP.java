package utils;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SFTP {

        public static final String PRIVATE_KEY_DIR = "src/main/resources/private_key";
        private final SftpConfig sftpConfig;

        public SFTPUtils(SftpConfig sftpConfig) {
            this.sftpConfig = sftpConfig;
        }

        public SftpConfig getSftpConfig() {
            return sftpConfig;
        }

        /**
         * Download latest file with comparator
         *
         * @param fileNamePrefix File name prefix
         * @param remoteDir      Remote directory
         * @param fileExtension  File type
         * @param localDir       Local directory to download file to
         * @param fileComparator Comparator
         * @return Downloaded file path
         * @throws JSchException JSCH Exception
         * @throws SftpException SFTP Exception
         * @throws IOException   IO Exception
         */
        public String downloadLatestFileWithComparator(String fileNamePrefix, String remoteDir, FileExtension fileExtension, String localDir, Comparator<ChannelSftp.LsEntry> fileComparator) throws JSchException, SftpException, IOException {
            return (String) this.execute((channelSftp) -> {
                Vector<ChannelSftp.LsEntry> files = channelSftp.ls(remoteDir);
                ChannelSftp.LsEntry latestEntry = files.stream().filter((entry) -> {
                    return !".".equals(entry.getFilename()) && !"..".equals(entry.getFilename());
                }).filter((entry) -> {
                    return entry.getFilename().toLowerCase().endsWith(fileExtension.getExtension());
                }).filter((entry) -> {
                    return entry.getFilename().startsWith(fileNamePrefix);
                }).peek((entry) -> {
                    log.info("Remote file: " + entry.getFilename());
                }).max(fileComparator).orElse(null);
                if (Objects.isNull(latestEntry)) {
                    return null;
                } else {
                    Path localDirPath = Paths.get(localDir);
                    if (!localDirPath.toFile().exists()) {
                        Files.createDirectory(localDirPath);
                    }

                    String downloadedFile = Paths.get(localDir, latestEntry.getFilename()).toString();
                    String remoteFileAbsPath = remoteDir + "/" + latestEntry.getFilename();
                    log.info("Downloading remote file " + remoteFileAbsPath);
                    channelSftp.get(remoteFileAbsPath, downloadedFile);
                    log.info("File downloaded at " + downloadedFile);
                    return downloadedFile;
                }
            });
        }

        /**
         * Call callback when connect to SFTP successfully
         *
         * @param callback Callback
         * @return Returned value
         * @throws SftpException SFTP Exception
         * @throws JSchException JSCH Exception
         * @throws IOException   IO Exception
         */
        public Object execute(SftpCallback callback) throws SftpException, JSchException, IOException {
            return this.execute(this.getSftpConfig(), callback);
        }

        @SneakyThrows
        private Object execute(SftpConfig config, SftpCallback callback) {
            JSch jSch = new JSch();
            Session session;
            if (config.isUsePhysicalKey() || config.getKeyContent() != null) {
                String pathFileKey;
                byte[] bytes;

                if (config.isUsePhysicalKey()) {
                    pathFileKey = PRIVATE_KEY_DIR + File.separator + "sftp_export_data_warehouse.ppk";
                    log.info("Private key path: " + pathFileKey);
                    jSch.addIdentity(pathFileKey);
                } else {
                    bytes = config.getKeyContent().getBytes();
                    jSch.addIdentity("private Key", bytes, null, null);
                }

                session = jSch.getSession(config.getUsername(), config.getHost(), config.getPort());
            } else {
                session = jSch.getSession(config.getUsername(), config.getHost(), config.getPort());
                if (config.getPassword() != null) {
                    session.setPassword(config.getPassword());
                }
            }
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            Object var5;
            try {
                var5 = callback.execute(channel);
            } catch (Exception var9) {
                log.error("SFTP error on execution: " + var9.getMessage());
                throw var9;
            } finally {
                channel.disconnect();
                session.disconnect();
            }

            return var5;
        }

        /**
         * Download file from remote then unzip if it is compressed file and then get a specific
         * file from unzipped files
         *
         * @param dstAbsolutePath Destination folder
         * @param fileExtension   File type
         * @param fileNamePrefix  File name prefix
         * @param localDir        Local directory where we get a file from
         * @return File
         * @throws Exception Exception
         */
        public File getFile(String dstAbsolutePath, FileExtension fileExtension, String fileNamePrefix, String localDir) throws Exception {
            String filePath = null;
            File file = new File(localDir + File.separator + fileNamePrefix + fileExtension.getExtension());
            if (!file.exists()) {
                log.info("No file available in local = " + localDir);
                log.info("Finding latest file from the remote = " + dstAbsolutePath);
                filePath = this.downloadLatestFile(fileNamePrefix, dstAbsolutePath, fileExtension, localDir);
                log.info("Downloaded latest file = " + filePath);
                if (fileNamePrefix.equalsIgnoreCase("SAMDataExtracts_")) {
                    log.info("Extracting the .zip file");
                    file = new File(localDir + fileNamePrefix + fileExtension.getExtension());
                } else {
                    file = new File(filePath);
                }
            }

            log.info("file.getPath() = " + file.getPath());
            return file;
        }

        /**
         * Download latest file
         *
         * @param fileNamePrefix File name prefix
         * @param remoteDir      Remote directory
         * @param fileExtension  File type
         * @param localDir       Local directory to download a file to
         * @return File path
         * @throws JSchException JSCH Exception
         * @throws SftpException SFTP Exception
         * @throws IOException   IO Exception
         */
        public String downloadLatestFile(String fileNamePrefix, String remoteDir, FileExtension fileExtension, String localDir) throws JSchException, SftpException, IOException {
            return this.downloadLatestFileWithComparator(fileNamePrefix, remoteDir, fileExtension, localDir, Comparator.comparing((o) -> {
                return this.getFileTimestamp(o.getFilename());
            }));
        }

        private String getFileTimestamp(String fileName) {
            return fileName.substring(fileName.lastIndexOf(95) + 1, fileName.indexOf(46));
        }

        /**
         * Wait for a file being uploaded to SFTP by comparing last modification time of files in SFTP
         * with current time
         *
         * @param fileNamePrefix            File name prefix
         * @param dstAbsolutePath           Destination folder
         * @param currentLatestModifiedTime Current time
         * @param minutes                   Wait time
         * @throws Exception Exception
         */
        public void waitForNewFileExportedToSftp(String fileNamePrefix, String dstAbsolutePath, String currentLatestModifiedTime, int minutes) throws Exception {
            Wrapper wrapper = new Wrapper();
            int milliSec = minutes * 60 * 1000;
            boolean flag = true;

            do {
                if (!flag) {
                    wrapper.holdOn(10000);
                    return;
                }

                for (int i = 0; i <= milliSec; i += 60000) {
                    wrapper.holdOn(60000);
                    String latestModifiedTime = this.getLatestModifiedTimeInSftpDirectory(fileNamePrefix, dstAbsolutePath);
                    if (!latestModifiedTime.equals(currentLatestModifiedTime)) {
                        flag = false;
                        break;
                    }
                }
            } while (!flag);

            throw new Error("No file is exported to sftp after " + minutes + " minutes!!!");
        }

        /**
         * Get last modification time
         *
         * @param filePrefix      File prefix
         * @param dstAbsolutePath Destination path
         * @return Last modified time
         * @throws IOException   IO Exception
         * @throws SftpException SFTP Exception
         * @throws JSchException JSCH Exception
         */
        public String getLatestModifiedTimeInSftpDirectory(String filePrefix, String dstAbsolutePath) throws IOException, SftpException, JSchException {
            ChannelSftp.LsEntry latestEntry = this.getLatestFileInSftpDirectory(filePrefix, dstAbsolutePath, FileExtension.ZIP);
            String latestModifiedTime;
            if (latestEntry != null) {
                log.info("Last Modify Time of prefix file " + filePrefix + ": " + latestEntry.getAttrs().getMtimeString());
                latestModifiedTime = latestEntry.getAttrs().getMtimeString();
            } else {
                latestModifiedTime = "";
                log.info("Sftp directory is empty!!!");
            }

            return latestModifiedTime;
        }

        /**
         * Get the latest file in SFTP folder
         *
         * @param fileNamePrefix File name prefix
         * @param remoteDir      Remote directory
         * @param fileExtension  File typ
         * @return Listed file entry
         * @throws JSchException JSCH Exception
         * @throws SftpException SFTP Exception
         * @throws IOException   IO Exception
         */
        public ChannelSftp.LsEntry getLatestFileInSftpDirectory(String fileNamePrefix, String remoteDir, FileExtension fileExtension) throws JSchException, SftpException, IOException {
            return (ChannelSftp.LsEntry) this.execute((channelSftp) -> {
                Vector<ChannelSftp.LsEntry> files = channelSftp.ls(remoteDir);
                ChannelSftp.LsEntry latestEntry = files.stream().filter((entry) -> {
                    return !".".equals(entry.getFilename()) && !"..".equals(entry.getFilename());
                }).filter((entry) -> {
                    return entry.getFilename().toLowerCase().endsWith(fileExtension.getExtension());
                }).filter((entry) -> {
                    return entry.getFilename().startsWith(fileNamePrefix);
                }).peek((entry) -> {
                    log.info("Found remote file: " + entry.getFilename());
                }).max(Comparator.comparingInt((o) -> {
                    return o.getAttrs().getMTime();
                })).orElse(null);
                return Objects.isNull(latestEntry) ? null : latestEntry;
            });
        }

        public File getFile(String remoteDir, String localDir, String remoteFileName) {
            try {
                return (File) this.execute((channelSftp) -> {
                    String remoteAbsPath = remoteDir + "/" + remoteFileName;
                    String localAbsPath = localDir + "/" + remoteFileName;
                    Path localDirPath = Paths.get(localDir);
                    if (!Files.exists(localDirPath)) {
                        log.info("Creating directory " + localDir);
                        Files.createDirectory(localDirPath);
                    }

                    log.info("Download from " + remoteAbsPath + " to " + localAbsPath);
                    channelSftp.get(remoteAbsPath, localAbsPath);
                    log.info("Download successfully to " + localAbsPath);
                    return new File(localAbsPath);
                });
            } catch (JSchException | IOException | SftpException var5) {
                var5.printStackTrace();
                return null;
            }
        }

        public void sftpSendFile(String srcAbsolutePath, String dstAbsolutePath) throws SftpException, IOException, InterruptedException {
            int retryConnectionCount = 3;
            boolean verify = false;

            while (!verify && retryConnectionCount > 0) {
                try {
                    this.execute((channelSftp) -> {
                        channelSftp.put(srcAbsolutePath, dstAbsolutePath);
                        return null;
                    });
                    verify = true;
                    log.info("Successfully uploaded file " + srcAbsolutePath + " to " + dstAbsolutePath);
                } catch (JSchException var6) {
                    log.error("sftpSendFile fail by error", var6);
                    log.info("retryConnectionCount = " + retryConnectionCount);
                    log.info("Sleeping 10 seconds ... ");
                    Thread.sleep(10000L);
                    --retryConnectionCount;
                }
            }

        }

        public boolean isSftpListFileExisted(String dstAbsolutePath, List<String> fileNames) throws InterruptedException {
            Iterator var3 = fileNames.iterator();

            boolean isExist;
            do {
                if (!var3.hasNext()) {
                    return true;
                }

                String filename = (String) var3.next();
                isExist = this.getFile(dstAbsolutePath + filename, 5);
            } while (isExist);

            return false;
        }

        public boolean isFileExistedOnSFTP(String dstAbsolutePath, File file) throws InterruptedException {
            String filename = file.getName();
            return this.getFile(dstAbsolutePath + "/" + filename, 3);
        }

        private boolean getFile(String dstAbsolutePath, int retryCount) throws InterruptedException {
            AtomicBoolean isExisted = new AtomicBoolean(false);
            while (!isExisted.get() && retryCount > 0) {
                try {
                    this.execute((channelSftp) -> {
                        channelSftp.stat(dstAbsolutePath);
                        isExisted.set(true);
                        return true;
                    });
                } catch (JSchException | IOException | SftpException var5) {
                    log.info("Given file is not available in SFTP folder yet. " + var5.getMessage());
                    log.info("retryCount = " + retryCount);
                    log.info("Sleeping 10 seconds ... ");
                    Thread.sleep(10000L);
                    --retryCount;
                }
            }

            return isExisted.get();
        }

        /**
         * Delete files in SFTP
         *
         * @param channelSftp SFTP channel
         * @param sftpFolder  SFTP folder
         * @param fileNames   File names
         */
        public void deleteFiles(ChannelSftp channelSftp, String sftpFolder, List<String> fileNames) {
            Iterator var4 = fileNames.iterator();

            while (var4.hasNext()) {
                String fileName = (String) var4.next();

                try {
                    channelSftp.rm(sftpFolder + "/" + fileName);
                    log.error("Delete file successfully: " + sftpFolder + "/" + fileName);
                } catch (Exception var7) {
                    log.error("Delete file error: " + var7.getMessage());
                }
            }

        }

        /**
         * Delete files
         *
         * @param channelSftp SFTP channel
         * @param fileNames   File names
         */
        private void deleteFiles(ChannelSftp channelSftp, List<String> fileNames) {
            Iterator var3 = fileNames.iterator();

            while (var3.hasNext()) {
                String fileName = (String) var3.next();

                try {
                    channelSftp.rm(fileName);
                    log.error("Delete file successfully: " + fileNames);
                } catch (Exception var6) {
                    log.error("Delete file error: " + var6.getMessage());
                }
            }

        }

        /**
         * Upload file to remote folder
         *
         * @param config     Configuration
         * @param sftpFolder SFTP folder
         * @param filePath   File path
         */
        public void sendFile(SftpConfig config, String sftpFolder, String filePath) {
            Session session = null;
            ChannelSftp channelSftp = null;
            int retry = 3;
            boolean successfull = false;

            while (!successfull && retry > 0) {
                try {
                    session = (new JSch()).getSession(config.getUsername(), config.getHost(), config.getPort());
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.setPassword(config.getPassword());
                    session.connect();
                    channelSftp = (ChannelSftp) session.openChannel("sftp");
                    channelSftp.connect();
                    channelSftp.put(filePath, sftpFolder);
                    successfull = true;
                } catch (Exception var15) {
                    log.error("SFTP error: " + var15.getMessage());
                    log.info("retry = " + retry);
                    log.info("Sleeping 10 seconds ... ");

                    try {
                        Thread.sleep(10000L);
                    } catch (InterruptedException var14) {
                        var14.printStackTrace();
                    }

                    --retry;
                } finally {
                    channelSftp.disconnect();
                    session.disconnect();
                }
            }

        }

        /**
         * Download file from SFTP
         *
         * @param config         Configuration
         * @param remoteDir      Remote folder
         * @param localDir       Local folder
         * @param remoteFileName Remote file name
         */
        public void getFile(SftpConfig config, String remoteDir, String localDir, String remoteFileName) {
            Session session = null;
            ChannelSftp channelSftp = null;
            int retry = 3;
            String remoteAbsPath = remoteDir + File.separator + remoteFileName;
            String localAbsPath = localDir + File.separator + remoteFileName;

            for (Path localDirPath = Paths.get(localDir); retry > 0; --retry) {
                try {
                    session = (new JSch()).getSession(config.getUsername(), config.getHost(), config.getPort());
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.setPassword(config.getPassword());
                    session.connect();
                    channelSftp = (ChannelSftp) session.openChannel("sftp");
                    channelSftp.connect();
                    if (!Files.exists(localDirPath)) {
                        log.info("Creating directory " + localDir);
                        Files.createDirectory(localDirPath);
                    }

                    channelSftp.get(remoteAbsPath, localAbsPath);
                } catch (Exception var14) {
                    log.error("SFTP error: " + var14.getMessage());
                    log.info("retry = " + retry);
                    log.info("Sleeping 10 seconds ... ");
                }

                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException var13) {
                    log.error(var13.getMessage());
                }
            }
        }
    }


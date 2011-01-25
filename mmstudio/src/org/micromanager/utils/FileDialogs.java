///////////////////////////////////////////////////////////////////////////////
//FILE:          FileDialogs.java
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//AUTHOR:        Arthur Edelstein, arthuredelstein@gmail.com January 2011
//COPYRIGHT:     University of California, San Francisco, 2011
//LICENSE:       This file is distributed under the BSD license.
//               License text is included with the source distribution.
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.utils;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;

public class FileDialogs {

   public static class FileType {
      final String name;
      final String[] suffixes;
      final String description;

      public FileType(String name, String description, String... suffixes) {
         this.name = name;
         this.description = description;
         this.suffixes = suffixes;
      }
   }

   private static class GeneralFileFilter
           extends javax.swing.filechooser.FileFilter
           implements java.io.FilenameFilter
   {
      private final String fileDescription_;
      private final String[] fileSuffixes_;
      public GeneralFileFilter(String fileDescription, final String [] fileSuffixes) {
         fileDescription_ = fileDescription;
         fileSuffixes_ = fileSuffixes;
      }
      @Override
      public boolean accept(File pathname) {
         String name = pathname.getName();
         int n = name.lastIndexOf(".");
         String suffix = name.substring(1+n).toLowerCase();
         for (int i=0; i<fileSuffixes_.length; ++i) {
            if (fileSuffixes_[i].toLowerCase().contentEquals(suffix))
               return true;
         }
         return false;
      }
      @Override
      public String getDescription() {
         return fileDescription_;
      }

      public boolean accept(File dir, String name) {
         return accept(new File(dir, name));
      }
   }

   public static File show(Window parent, String title, File startFile,
                    boolean selectDirectories, boolean load,
                    final String fileDescription,
                    final String[] fileSuffixes) {
      File selectedFile = null;
      GeneralFileFilter filter = new GeneralFileFilter(fileDescription, fileSuffixes);

      if (JavaUtils.isMac()) {
         if (selectDirectories) {
         // For Mac we only select directories, unfortunately!
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
         }
         int mode = load? FileDialog.LOAD : FileDialog.SAVE;
         FileDialog fd;
         if (parent instanceof Dialog) {
            fd = new FileDialog((Dialog) parent, title, mode);
         } else if (parent instanceof Frame) {
            fd = new FileDialog((Frame) parent, title, mode);
         } else {
            fd = new FileDialog((Dialog) null, title, mode);
         }
         if (startFile != null) {
            if (load)
               fd.setDirectory(startFile.getParent());
            else {
               fd.setFile(startFile.getAbsolutePath());
            }
         }
         if (fileSuffixes != null) {
            fd.setFilenameFilter(filter);
         }
         fd.setVisible(true);
         if (selectDirectories) {
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
         }
         if (fd.getFile() != null) {
           selectedFile = new File(fd.getDirectory() + "/" + fd.getFile());
            if (mode == FileDialog.SAVE) {
               if (! filter.accept(selectedFile)) {
                  selectedFile = new File(selectedFile.getAbsolutePath()
                                          + "." + fileSuffixes[0]);
               }
            }
         }

         fd.dispose();
      } else {
         JFileChooser fc;
         if (startFile != null) {
            if (load) {
               fc = new JFileChooser(startFile.getParentFile());
            } else {
               fc = new JFileChooser(startFile);
            }
         } else {
            fc = new JFileChooser();
         }
         fc.setDialogTitle(title);
         if (selectDirectories) {
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
         }
         if (fileSuffixes != null) {
            fc.setFileFilter(filter);
         }
         int returnVal;
         if (load) {
            returnVal = fc.showOpenDialog(parent);
         } else {
            returnVal = fc.showSaveDialog(parent);
         }
         if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
         }
      }
      return selectedFile;
   }

   public static File show(Window parent, String title, FileType type,
                    boolean selectDirectories, boolean load) {
      Preferences node = Preferences.userNodeForPackage(FileDialogs.class);
      String startDirStr = node.get(type.name, null);
      File startDir = null;
      if (startDirStr != null) {
         startDir = new File(startDirStr);
      }
      File result = show(parent, title, startDir, selectDirectories, load,
                         type.description, type.suffixes);
      if (result != null) {
         File dir = result.getParentFile();
         node.put(type.name, dir.getAbsolutePath());
      }
      return result;
   }

   public static File openFile(Window parent, String title, FileType type) {
      return show(parent, title, type, false, true);
   }

   public static File openDir(Window parent, String title, FileType type) {
      return show(parent, title, type, true, true);
   }

   public static File save(Window parent, String title, FileType type) {
      return show(parent, title, type, false, false);
   }
}

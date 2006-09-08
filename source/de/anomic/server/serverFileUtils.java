// serverFileUtils.java 
// -------------------------------------------
// (C) by Michael Peter Christen; mc@anomic.de
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
// last major change: 05.08.2004
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Using this software in any meaning (reading, learning, copying, compiling,
// running) means that you agree that the Author(s) is (are) not responsible
// for cost, loss of data or any harm that may be caused directly or indirectly
// by usage of this softare or this documentation. The usage of this software
// is on your own risk. The installation and usage (starting/running) of this
// software may allow other people or application to access your computer and
// any attached devices and is highly dependent on the configuration of the
// software which must be done by the user of the software; the author(s) is
// (are) also not responsible for proper configuration and usage of the
// software, even if provoked by documentation provided together with
// the software.
//
// Any changes to this file according to the GPL as documented in the file
// gpl.txt aside this file in the shipment you received can be done to the
// lines that follows this copyright notice here, but changes must not be
// done inside the copyright notive above. A re-distribution must contain
// the intact and unchanged copyright notice.
// Contributions and changes to the program code must be marked as such.

package de.anomic.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Iterator;

import de.anomic.kelondro.kelondroRow;
import de.anomic.kelondro.kelondroRowSet;

public final class serverFileUtils {

    /**
    * Copies an InputStream to an OutputStream.
    * @param source    InputStream
    * @param dest    OutputStream
    * @return Total number of bytes copied.
    * @see copy(InputStream source, File dest)
    * @see copyRange(File source, OutputStream dest, int start)
    * @see copy(File source, OutputStream dest)
    * @see copy(File source, File dest)
    */
    public static int copy(InputStream source, OutputStream dest) throws IOException {
        byte[] buffer = new byte[4096];
        
        int c, total = 0;
        while ((c = source.read(buffer)) > 0) {
            dest.write(buffer, 0, c);
            dest.flush();
            total += c;
        }
        dest.flush();
        
        return total;
    }

    /**
    * Copies an InputStream to a File.
    * @param source    InputStream
    * @param dest    File
    * @see copy(InputStream source, OutputStream dest)
    * @see copyRange(File source, OutputStream dest, int start)
    * @see copy(File source, OutputStream dest)
    * @see copy(File source, File dest)
    */
    public static void copy(InputStream source, File dest) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dest);
            copy(source, fos);
        } finally {
            if (fos != null) try {fos.close();} catch (Exception e) {}
        }
    }

    /**
    * Copies a part of a File to an OutputStream.
    * @param source    File
    * @param dest    OutputStream
    * @param start Number of bytes to skip from the beginning of the File
    * @see copy(InputStream source, OutputStream dest)
    * @see copy(InputStream source, File dest)
    * @see copy(File source, OutputStream dest)
    * @see copy(File source, File dest)
    */
    public static void copyRange(File source, OutputStream dest, int start) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(source);
            long skipped = fis.skip(start);
            if (skipped != start) throw new IllegalStateException("Unable to skip '" + start + "' bytes. Only '" + skipped + "' bytes skipped.");
            copy(fis, dest);
        } finally {
            if (fis != null) try { fis.close(); } catch (Exception e) {}
        }
    }

    /**
    * Copies a File to an OutputStream.
    * @param source    File
    * @param dest    OutputStream
    * @see copy(InputStream source, OutputStream dest)
    * @see copy(InputStream source, File dest)
    * @see copyRange(File source, OutputStream dest, int start)
    * @see copy(File source, File dest)
    */
    public static void copy(File source, OutputStream dest) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(source);
            copy(fis, dest);
        } finally {
            if (fis != null) try { fis.close(); } catch (Exception e) {}
        }
    }

    /**
    * Copies a File to a File.
    * @param source    File
    * @param dest    File
    * @see copy(InputStream source, OutputStream dest)
    * @see copy(InputStream source, File dest)
    * @see copyRange(File source, OutputStream dest, int start)
    * @see copy(File source, OutputStream dest)
    */
    public static void copy(File source, File dest) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(dest);
            copy(fis, fos);
        } finally {
            if (fis != null) try {fis.close();} catch (Exception e) {}
            if (fos != null) try {fos.close();} catch (Exception e) {}
        }
    }

    public static byte[] read(InputStream source) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(source, baos);
        baos.close();
        return baos.toByteArray();
    }

    public static byte[] read(File source) throws IOException {
        byte[] buffer = new byte[(int) source.length()];
        InputStream fis = null;
        try {
            fis = new FileInputStream(source);
            int p = 0, c;
            while ((c = fis.read(buffer, p, buffer.length - p)) > 0) p += c;
        } finally {
            if (fis != null) try { fis.close(); } catch (Exception e) {}
        }
        return buffer;
    }

    public static byte[] readAndZip(File source) throws IOException {
        ByteArrayOutputStream byteOut = null;
        GZIPOutputStream zipOut = null;
        try {
            byteOut = new ByteArrayOutputStream((int)(source.length()/2));
            zipOut = new GZIPOutputStream(byteOut);
            copy(source, zipOut);
            zipOut.close();
            return byteOut.toByteArray();
        } finally {
            if (zipOut != null) try { zipOut.close(); } catch (Exception e) {}
            if (byteOut != null) try { byteOut.close(); } catch (Exception e) {}
        }
    }

    public static void writeAndGZip(byte[] source, File dest) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dest);
            writeAndGZip(source, fos);
        } finally {
            if (fos != null) try {fos.close();} catch (Exception e) {}
        }
    }

    public static void writeAndGZip(byte[] source, OutputStream dest) throws IOException {
        GZIPOutputStream zipOut = null;
        try {
            zipOut = new GZIPOutputStream(dest);
            write(source, zipOut);
            zipOut.close();
        } finally {
            if (zipOut != null) try { zipOut.close(); } catch (Exception e) {}
        }
    }

    public static void write(byte[] source, OutputStream dest) throws IOException {
        copy(new ByteArrayInputStream(source), dest);
    }

    public static void write(byte[] source, File dest) throws IOException {
        copy(new ByteArrayInputStream(source), dest);
    }

    public static HashSet loadList(File file) {
        HashSet set = new HashSet();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if ((line.length() > 0) && (!(line.startsWith("#")))) set.add(line.trim().toLowerCase());
            }
            br.close();
        } catch (IOException e) {
        } finally {
            if (br != null) try { br.close(); } catch (Exception e) {}
        }
        return set;
    }

    public static Map loadHashMap(File f) {
        // load props
        Properties prop = new Properties();
        BufferedInputStream bufferedIn = null;
        try {
            prop.load(bufferedIn = new BufferedInputStream(new FileInputStream(f)));
        } catch (IOException e1) {
            System.err.println("ERROR: " + f.toString() + " not found in settings path");
            prop = null;
        } finally {
            if (bufferedIn != null)try{bufferedIn.close();}catch(Exception e){}
        }
        return (Hashtable) prop;
    }

    public static void saveMap(File file, Map props, String comment) throws IOException {
        PrintWriter pw = null;
        File tf = new File(file.toString() + "." + (System.currentTimeMillis() % 1000));
        pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(tf)));
        pw.println("# " + comment);
        Iterator i = props.entrySet().iterator();
        String key, value;
        Map.Entry entry;
        while (i.hasNext()) {
            entry  = (Map.Entry) i.next();
            key = (String) entry.getKey();
            value = ((String) entry.getValue()).replaceAll("\n", "\\\\n");
            pw.println(key + "=" + value);
        }
        pw.println("# EOF");
        pw.close();
        file.delete();
        tf.renameTo(file);
    }

    public static Set loadSet(File file, int chunksize, boolean tree) throws IOException {
        Set set = (tree) ? (Set) new TreeSet() : (Set) new HashSet();
        byte[] b = read(file);
        for (int i = 0; (i + chunksize) <= b.length; i++) {
            set.add(new String(b, i, chunksize));
        }
        return set;
    }

    public static Set loadSet(File file, String sep, boolean tree) throws IOException {
        Set set = (tree) ? (Set) new TreeSet() : (Set) new HashSet();
        byte[] b = read(file);
        StringTokenizer st = new StringTokenizer(new String(b, "UTF-8"), sep);
        while (st.hasMoreTokens()) {
            set.add(st.nextToken());
        }
        return set;
    }

    public static void saveSet(File file, String format, Set set, String sep) throws IOException {
        File tf = new File(file.toString() + ".tmp" + (System.currentTimeMillis() % 1000));
        OutputStream os = null;
        if ((format == null) || (format.equals("plain"))) {
            os = new BufferedOutputStream(new FileOutputStream(tf));
        } else if (format.equals("gzip")) {
            os = new GZIPOutputStream(new FileOutputStream(tf));
        } else if (format.equals("zip")) {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
            String name = file.getName();
            if (name.endsWith(".zip")) name = name.substring(0, name.length() - 4);
            zos.putNextEntry(new ZipEntry(name + ".txt"));
            os = zos;
        }
        for (Iterator i = set.iterator(); i.hasNext(); ) {
            os.write((i.next().toString()).getBytes());
            if (sep != null) os.write(sep.getBytes());
        }
        os.close();
        file.delete();
        tf.renameTo(file);
    }

    public static void saveSet(File file, String format, kelondroRowSet set, String sep) throws IOException {
        File tf = new File(file.toString() + ".tmp" + (System.currentTimeMillis() % 1000));
        OutputStream os = null;
        if ((format == null) || (format.equals("plain"))) {
            os = new BufferedOutputStream(new FileOutputStream(tf));
        } else if (format.equals("gzip")) {
            os = new GZIPOutputStream(new FileOutputStream(tf));
        } else if (format.equals("zip")) {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
            String name = file.getName();
            if (name.endsWith(".zip")) name = name.substring(0, name.length() - 4);
            zos.putNextEntry(new ZipEntry(name + ".txt"));
            os = zos;
        }
        Iterator i = set.rows();
        String key;
        if (i.hasNext()) {
            key = new String(((kelondroRow.Entry) i.next()).getColBytes(0));
            os.write(key.getBytes());
        }
        while (i.hasNext()) {
            key = new String(((kelondroRow.Entry) i.next()).getColBytes(0));
            if (sep != null) os.write(sep.getBytes());
            os.write(key.getBytes());
        }
        os.close();
        file.delete();
        tf.renameTo(file);
    }
    
    /**
    * Moves all files from a directory to another.
    * @param from_dir    Directory which contents will be moved.
    * @param to_dir    Directory to move into. It must exist already.
    */
    public static void moveAll(File from_dir, File to_dir) {
        if (!(from_dir.isDirectory())) return;
        if (!(to_dir.isDirectory())) return;
        String[] list = from_dir.list();
        for (int i = 0; i < list.length; i++) (new File(from_dir, list[i])).renameTo(new File(to_dir, list[i]));
    }

    public static void main(String[] args) {
        try {
            writeAndGZip("ein zwei drei, Zauberei".getBytes(), new File("zauberei.txt.gz"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

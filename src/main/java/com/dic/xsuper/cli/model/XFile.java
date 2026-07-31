package com.dic.xsuper.cli.model;

import java.io.File;
import java.net.URI;

public class XFile extends File {

    public XFile(String pathname) {
        super(pathname);
    }

    public XFile(String parent, String child) {
        super(parent, child);
    }

    public XFile(File parent, String child) {
        super(parent, child);
    }

    public XFile(URI uri) {
        super(uri);
    }
}
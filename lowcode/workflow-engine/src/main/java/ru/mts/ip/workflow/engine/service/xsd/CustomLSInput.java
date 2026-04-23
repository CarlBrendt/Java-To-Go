package ru.mts.ip.workflow.engine.service.xsd;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import org.w3c.dom.ls.LSInput;

public class CustomLSInput implements LSInput {

  private String publicId;
  private String systemId;
  private String content;

  public CustomLSInput(String publicId, String systemId, String content) {
    this.publicId = publicId;
    this.systemId = systemId;
    this.content = content;
  }

  @Override
  public Reader getCharacterStream() {
    return new StringReader(content);
  }

  @Override
  public void setCharacterStream(Reader reader) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getByteStream() {
    return null;
  }

  @Override
  public void setByteStream(InputStream inputStream) {}

  @Override
  public String getStringData() {
    return null;
  }

  @Override
  public void setStringData(String string) {}

  @Override
  public String getSystemId() {
    return systemId;
  }

  @Override
  public void setSystemId(String systemId) {
    this.systemId = systemId;
  }

  @Override
  public String getPublicId() {
    return publicId;
  }

  @Override
  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  @Override
  public String getBaseURI() {
    return null;
  }

  @Override
  public void setBaseURI(String baseURI) {}

  @Override
  public String getEncoding() {
    return null;
  }

  @Override
  public void setEncoding(String encoding) {}

  @Override
  public boolean getCertifiedText() {
    return false;
  }

  @Override
  public void setCertifiedText(boolean certifiedText) {}
}

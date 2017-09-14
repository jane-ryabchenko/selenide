package com.codeborne.selenide;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ThreadLocalConfiguration {
  public interface ConfigurationInterface {
    String getBaseUrl();
    void setBaseUrl(String baseUrl);
    long getCollectionsTimeout();
    void setCollectionsTimeout(long collectionsTimeout);
    long getTimeout();
    void setTimeout(long timeout);
    long getPollingInterval();
    void setPollingInterval(long pollingInterval);
    long getCollectionsPollingInterval();
    void setCollectionsPollingInterval(long collectionsPollingInterval);
    boolean isHoldBrowserOpen();
    void setHoldBrowserOpen(boolean holdBrowserOpen);
    boolean isReopenBrowserOnFail();
    void setReopenBrowserOnFail(boolean reopenBrowserOnFail);
    long getOpenBrowserTimeoutMs();
    void setOpenBrowserTimeoutMs(long openBrowserTimeoutMs);
    long getCloseBrowserTimeoutMs();
    void setCloseBrowserTimeoutMs(long closeBrowserTimeoutMs);
    String getBrowser();
    void setBrowser(String browser);
    String getBrowserVersion();
    void setBrowserVersion(String browserVersion);
    String getRemote();
    void setRemote(String remote);
    String getBrowserSize();
    void setBrowserSize(String browserSize);
    boolean isStartMaximized();
    void setStartMaximized(boolean startMaximized);
    String getPageLoadStrategy();
    void setPageLoadStrategy(String pageLoadStrategy);
    boolean isClickViaJs();
    void setClickViaJs(boolean clickViaJs);
    boolean isCaptureJavascriptErrors();
    void setCaptureJavascriptErrors(boolean captureJavascriptErrors);
    boolean isScreenshots();
    void setScreenshots(boolean screenshots);
    boolean isSavePageSource();
    void setSavePageSource(boolean savePageSource);
    String getReportsFolder();
    void setReportsFolder(String reportsFolder);
    String getReportsUrl();
    void setReportsUrl(String reportsUrl);
    boolean isDismissModalDialogs();
    void setDismissModalDialogs(boolean dismissModalDialogs);
    boolean isFastSetValue();
    void setFastSetValue(boolean fastSetValue);
    boolean isVersatileSetValue();
    void setVersatileSetValue(boolean versatileSetValue);
    Configuration.SelectorMode getSelectorMode();
    void setSelectorMode(Configuration.SelectorMode selectorMode);
    Configuration.AssertionMode getAssertionMode();
    void setAssertionMode(Configuration.AssertionMode assertionMode);
    Configuration.FileDownloadMode getFileDownload();
    void setFileDownload(Configuration.FileDownloadMode fileDownload);
  }

  static class ConfigurationInvocationHandler implements InvocationHandler {
    private final static Object NULL_VALUE = new Object();
    private final static Pattern PARAMETER_REGEX = Pattern.compile("(get|is|set)(.+)");
    private final static String SETTER_PREFIX = "set";

    private ThreadLocal<Map<String, Object>> parameters = new ThreadLocal<Map<String, Object>>() {
      @Override
      protected Map<String, Object> initialValue() {
        return new HashMap<>();
      }
    };

    @Override
    public Object invoke(Object o, Method method, Object[] parameters) throws Throwable {
      String[] descriptor = parseAndValidateMethod(method);
      if (descriptor[0].equals(SETTER_PREFIX)) {
        return writeGlobalConfigurationProperty(descriptor[1], parameters[0]);
      }
      return readGlobalConfigurationProperty(descriptor[1]);
    }

    private String[] parseAndValidateMethod(Method method) {
      Matcher matcher = PARAMETER_REGEX.matcher(method.getName());
      checkState(matcher.find(),
          "Unsupported method name [%s]. ConfigurationInterface methods should have format /(get|is|set)(.+)/g",
          method.getName());
      String prefix = matcher.group(1);
      String property = matcher.group(2);
      if (Character.isUpperCase(property.charAt(0))) {
        property = Character.toLowerCase(property.charAt(0)) + property.substring(1, property.length());
      }

      if (prefix.equals(SETTER_PREFIX)) {
        checkState(method.getParameterCount() == 1 && method.getReturnType().getName().equals("void"),
            "Expected method [void %s(1)], actual method was [%s %s(%s)]",
            method.getName(),
            method.getReturnType().getSimpleName(),
            method.getName(),
            method.getParameterCount());
      } else {
        checkState(method.getParameterCount() == 0 && !method.getReturnType().getName().equals("void"),
            "Expected method [%s %s(0)], actual method was [%s %s(%s)]",
            method.getReturnType().getSimpleName(),
            method.getName(),
            method.getReturnType().getSimpleName(),
            method.getName(),
            method.getParameterCount());
      }
      return new String[] {prefix, property};
    }

    private Object readGlobalConfigurationProperty(String property)
        throws NoSuchFieldException, IllegalAccessException {
      Object value = this.parameters.get().get(property);

      if (value == null) {
        Field field = checkNotNull(Configuration.class.getField(property),
            "Global property [Configuration.%s] does not exist", property);
        checkState(Modifier.isStatic(field.getModifiers()),
            "Can't read global value of non-static property [Configuration.%s]", property);

        value = field.get(null);
      } else if (value == NULL_VALUE) {
        value = null;
      }
      return value;
    }

    private Object writeGlobalConfigurationProperty(String property, Object value) {
      this.parameters.get().put(property, value == null ? NULL_VALUE : value);
      return null;
    }
  }

  private static ConfigurationInterface instance =
      (ConfigurationInterface) Proxy.newProxyInstance(
          ClassLoader.getSystemClassLoader(),
          new Class<?>[] {ConfigurationInterface.class},
          new ConfigurationInvocationHandler());

  private ThreadLocalConfiguration() {}

  public static ConfigurationInterface get() {
    return instance;
  }
}

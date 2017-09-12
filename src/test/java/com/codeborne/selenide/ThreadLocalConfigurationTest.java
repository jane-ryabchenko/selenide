package com.codeborne.selenide;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ThreadLocalConfigurationTest {
  interface TestConfigurationInterface {
    long getTimeout();
    void setTimeout(long timeout);

    String property();
    void getProperty(long a);
    long setProperty(String a, String b);
  }

  private TestConfigurationInterface instance;

  @Before
  public void testBefore() {
    instance =
        (TestConfigurationInterface) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{TestConfigurationInterface.class},
            new ThreadLocalConfiguration.ConfigurationInvocationHandler());
  }

  @Test
  public void testOverride() {
    Configuration.baseUrl = "A";
    assertEquals("A", ThreadLocalConfiguration.get().getBaseUrl());

    ThreadLocalConfiguration.get().setBaseUrl("C");
    assertEquals("C", ThreadLocalConfiguration.get().getBaseUrl());

    Configuration.baseUrl = "B";
    assertEquals("C", ThreadLocalConfiguration.get().getBaseUrl());
  }

  @Test
  public void testInvocation() {
    Configuration.timeout = 1234L;
    assertEquals(1234L, instance.getTimeout());

    instance.setTimeout(123L);
    assertEquals(123L, instance.getTimeout());

    Configuration.timeout = 12345L;
    assertEquals(123L, instance.getTimeout());
  }

  @Test
  public void testInvocationValidation_wrongFormat() {
    Throwable exception = null;
    try {
      instance.property();
    } catch (Throwable t) {
      exception = t;
    }
    assertNotNull(exception);
    assertEquals(IllegalStateException.class, exception.getClass());
    assertEquals(
        "Unsupported method name [property]. ConfigurationInterface methods should have format /(get|is|set)(.+)/g",
        exception.getMessage());
  }

  @Test
  public void testInvocationValidation_wrongGetter() {
    Throwable exception = null;
    try {
      instance.getProperty(0);
    } catch (Throwable t) {
      exception = t;
    }
    assertNotNull(exception);
    assertEquals(IllegalStateException.class, exception.getClass());
    assertEquals(
        "Expected method [void getProperty(0)], actual method was [void getProperty(1)]", exception.getMessage());
  }

  @Test
  public void testInvocationValidation_wrongSetter() {
    Throwable exception = null;
    try {
      instance.setProperty("A", "B");
    } catch (Throwable t) {
      exception = t;
    }
    assertNotNull(exception);
    assertEquals(IllegalStateException.class, exception.getClass());
    assertEquals(
        "Expected method [void setProperty(1)], actual method was [long setProperty(2)]", exception.getMessage());
  }
}

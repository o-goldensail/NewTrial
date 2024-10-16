package ut.jira.phinia.tciz.reports;

import org.junit.Test;
import jira.phinia.tciz.reports.api.MyPluginComponent;
import jira.phinia.tciz.reports.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}
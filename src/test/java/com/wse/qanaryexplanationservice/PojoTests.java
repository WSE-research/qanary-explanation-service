package com.wse.qanaryexplanationservice;

import com.wse.qanaryexplanationservice.helper.pojos.QanaryComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class PojoTests {

    @Test
    public void testCorrectPrefixingWithPlainName() {
        String componentName = "test";
        QanaryComponent qanaryComponent = new QanaryComponent(componentName);

        Assertions.assertEquals(componentName, qanaryComponent.getComponentName());
        Assertions.assertEquals("urn:qanary:" + componentName, qanaryComponent.getPrefixedComponentName());
    }

    @Test
    public void testCorrectPrefixingWithPrefixedName() {
        String componentName = "urn:qanary:test";
        QanaryComponent qanaryComponent = new QanaryComponent(componentName);

        Assertions.assertEquals(componentName, qanaryComponent.getPrefixedComponentName());
        Assertions.assertEquals("test", qanaryComponent.getComponentName());
    }

}

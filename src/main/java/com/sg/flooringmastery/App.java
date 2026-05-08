package com.sg.flooringmastery;

import com.sg.flooringmastery.controller.FlooringMasteryController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Entry point for the application.
 * Loads the Spring config from applicationContext.xml, grabs the controller bean,
 * and calls run() to start the program.
 */
public class App {

    public static void main(String[] args) {
        ApplicationContext ctx =
                new ClassPathXmlApplicationContext("applicationContext.xml");
        FlooringMasteryController controller =
                ctx.getBean("controller", FlooringMasteryController.class);
        controller.run();
    }
}

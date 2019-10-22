package com.github.bric3.spring.autowire;

import org.springframework.beans.factory.annotation.Qualifier;

@Qualifier("excludedBean")
public class BeanExcluded extends ParentBean {}
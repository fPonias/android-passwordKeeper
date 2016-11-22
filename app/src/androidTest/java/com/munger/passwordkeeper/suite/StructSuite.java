package com.munger.passwordkeeper.suite;

import com.munger.passwordkeeper.struct.AES256Test;
import com.munger.passwordkeeper.struct.PasswordDetailsPairTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

// Runs all unit tests.
@RunWith(Suite.class)
@Suite.SuiteClasses({AES256Test.class, PasswordDetailsPairTest.class})
public class StructSuite {}

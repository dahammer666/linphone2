import argparse
import os
from bs4 import BeautifulSoup
from junit_xml import TestSuite, TestCase

parser = argparse.ArgumentParser('html2xml',description='html2xml - Python script to convert html androidTest reports into jUnit xml reports')
parser.add_argument('-p', '--reports-path',
                    help='The path to search for reports files',
                    required=False,
                    dest='path')
args = parser.parse_args()

path = "" if args.path == None else args.path

def findDiv(content,divText):
    tabs = content.find("div", {"id": "tabs"})
    tablinks = tabs.find("ul", {"class": "tabLinks"})
    tabIndex = tablinks.find("a",text=divText)["href"]
    div = content.find("div", {"id": tabIndex[1:]})
    return div

with open(os.path.join(path,"index.html")) as fp:
    indexContent = BeautifulSoup(fp, "html.parser")
    
classes = findDiv(indexContent,"Classes")
classNames = [a["href"] for a in classes.find_all("a")]

#name,state,time
Tests = {}
for file in classNames:
    with open(os.path.join(path,file)) as fp:
        classContent = BeautifulSoup(fp, "html.parser")
    fileName = ".".join(file.split(".")[:-1])
    Tests[fileName] = {}

    tests = findDiv(classContent,"Tests")
    for thead in tests.find_all("thead"): 
        thead.decompose()
    tests = tests.find_all("tr")
    for test in tests:
        testTd = test.find("td",{"class": None})
        testName = testTd.text
        testTd.decompose()
        testState = test.find("td")["class"][0]
        testTime = test.find("td").text.split("(")[1].split("s")[0].split("m")
        testTime = float(testTime[0]) if len(testTime) < 2 else int(testTime[0])*60 + float(testTime[1])
        Tests[fileName][testName] = [testState,testTime,None,None]

    try:
        failures = findDiv(classContent,"Failed tests").find_all("div",{"class":"test"})

        for failure in failures:
            testName = failure.find("a")["name"]
            message = failure.find("pre")
            images = []
            for img in message.find_all("img"):
                images.append(os.path.join(path,img["src"]))
                img.decompose()
            for div in message.find_all("div"):
                div.decompose()
            Tests[fileName][testName][2] = message.text
            Tests[fileName][testName][3] = "[[ATTACHMENT|" + " ".join(images) + "]]"
    except:
        pass

testSuites = []
for testClass in Tests:
    testCases = []
    failures = 0
    for testCase in Tests[testClass]:
        test = Tests[testClass][testCase]
        testCase = TestCase(testCase,testClass,test[1],test[3],status=test[0])
        if test[2] != None: 
            failures += 1
            testCase.add_failure_info(test[2])
        testCases.append(testCase)
    testSuites.append(TestSuite(testClass,testCases))

data = TestSuite.to_xml_string(testSuites)

file = open(os.path.join(path,"result.xml"),"w")
file.write(data)
file.close()







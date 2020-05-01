######################
#      Makefile      #
######################

FILE_NAME = documentation.tex

LATEX = xelatex
BIBER = biber
RUSTY_SWAGGER = rusty-swagger

.PHONY: scan-dependencies show-vulnerabilities-report

OPEN = open

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
   OPEN = xdg-open
endif

all: clean all1
all1: clean updateproject swagger la la2 la3
no: clean updateproject swagger la la2
docker-build: updateproject docker

updateproject:
	mvn -f dpppt-backend-sdk/pom.xml install
	#cp dpppt-backend-sdk/dpppt-backend-sdk-ws/generated/swagger/swagger.yaml documentation/yaml/sdk.yaml

swagger:
	cd documentation; $(RUSTY_SWAGGER) --file ../dpppt-backend-sdk/dpppt-backend-sdk-ws/generated/swagger/swagger.yaml

la:
	cd documentation;$(LATEX) $(FILE_NAME)
bib:
	cd documentation;$(BIBER) $(FILE_NAME)
la2:
	cd documentation;$(LATEX) $(FILE_NAME)
la3:
	cd documentation;$(LATEX) $(FILE_NAME)
show:
	cd documentation; $(OPEN) $(FILE_NAME).pdf &

docker:
	cp dpppt-backend-sdk/dpppt-backend-sdk-ws/target/dpppt-backend-sdk-ws-1.0.0-SNAPSHOT.jar ws-sdk/ws/bin/dpppt-backend-sdk-ws-1.0.0.jar
	docker build -t 979586646521.dkr.ecr.eu-west-1.amazonaws.com/ubiquevscovid19-ws:test ws-sdk/

clean:
	@rm -f documentation/*.log documentation/*.aux documentation/*.dvi documentation/*.ps documentation/*.blg documentation/*.bbl documentation/*.out documentation/*.bcf documentation/*.run.xml documentation/*.fdb_latexmk documentation/*.fls documentation/*.toc

scan-dependencies:
	cd dpppt-backend-sdk; mvn verify

show-vulnerabilities-report:
	cd dpppt-backend-sdk; $(OPEN) dpppt-backend-sdk-model/target/dependency-check-report.html

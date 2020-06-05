######################
#      Makefile      #
######################

FILE_NAME = documentation.tex

LATEX = xelatex
BIBER = biber
RUSTY_SWAGGER = rusty-swagger

all: clean all1
all1: clean updateproject updatedoc swagger la la2 la3 
no: clean updateproject updatedoc swagger la la2 
docker-build: updateproject docker
doc: updatedoc swagger la la2 la3

updateproject:
	mvn -f dpppt-backend-sdk/pom.xml install
updatedoc:
	mvn springboot-swagger-3:springboot-swagger-3 -f dpppt-backend-sdk/dpppt-backend-sdk-ws/pom.xml
	cp dpppt-backend-sdk/dpppt-backend-sdk-ws/generated/swagger/swagger.yaml documentation/yaml/sdk.yaml

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
	cd documentation; open $(FILE_NAME).pdf &

docker:
	cp dpppt-backend-sdk/dpppt-backend-sdk-ws/target/dpppt-backend-sdk-ws-1.0.0-SNAPSHOT.jar ws-sdk/ws/bin/dpppt-backend-sdk-ws-1.0.0.jar
	docker build -t 979586646521.dkr.ecr.eu-west-1.amazonaws.com/ubiquevscovid19-ws:test ws-sdk/
	


clean:
	@rm -f documentation/*.log documentation/*.aux documentation/*.dvi documentation/*.ps documentation/*.blg documentation/*.bbl documentation/*.out documentation/*.bcf documentation/*.run.xml documentation/*.fdb_latexmk documentation/*.fls documentation/*.toc

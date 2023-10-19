# Kill all java processes


# Start Qanary pipeline
cd Qanary/qanary_pipeline-template
nohup java -jar target/*.jar &

# Start components
cd ../../Qanary-question-answering-components
cd qanary-component-NED-DBpediaSpotlight
nohup java -jar target/*.jar &

cd ..
cd qanary-component-NED-Dandelion
nohup java -jar target/*.jar &

cd ..
cd qanary-component-NED-Tagme
nohup java -jar target/*.jar &

cd ..
cd qanary-component-NED-Ontotext
nohup java -jar target/*.jar &

# Start NER components
cd ..
cd qanary-component-NER-Dandelion
nohup java -jar target/*.jar &

cd ..
cd qanary-component-NER-DBpediaSpotlight
nohup java -jar target/*.jar &

cd ..
cd qanary-component-NER-TextRazor
nohup java -jar target/*.jar &

cd ..
cd qanary-component-NER-Tagme
nohup java -jar target/*.jar &

# Start QB components

cd ..
cd qanary-component-QB-Sina
nohup java -jar target/*.jar &

cd ..
cd qanary-component-QB-PlatypusWrapper
nohup java -jar target/*.jar &

cd ..
cd qanary-component-QBE-QAnswer
nohup java -jar target/*.jar &

# Start LD-Shuyo component

cd ..
cd qanary-component-LD-Shuyo
nohup java -jar target/*.jar &

# Start REL component(s)

cd ..
cd qanary-component-REL-Python-Falcon
docker-compose up --detach

# Start QE component(s)

# Already existing
#cd ..
#cd qanary-component-QBE-QAnswer
#nohup java -jar target/*.jar &

cd ..
cd qanary-component-QE-SparqlExecuter
nohup java -jar target/*.jar 
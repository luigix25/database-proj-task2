import json
import datetime
import csv
import re

timestamp_format 	= "%Y-%m-%d %H:%M:%S"
city				= "Chicago"
city_tag			= "CHI"

with open('../datasets/chicago.csv') as csvfile:
	reader = csv.DictReader(csvfile)
	count = 0
	for row in reader:
		normalized_row 	= {}
		time 			= {}
		space 			= {}
		rider 			= {}

		count += 1
		year = 2015 + count % 4
		time["year"] = year
		time["timestamp_start"] = re.sub('201\\d', str(year), row["starttime"])
		time["timestamp_end"] 	= re.sub('201\\d', str(year), row["stoptime"])

		space["station_start"] 	= city_tag +":"+ row["from_station_id"]
		space["station_end"] 	= city_tag +":"+ row["to_station_id"]

		space["latitude_start"] = row["latitude_start"]
		space["latitude_end"] 	= row["latitude_end"]

		space["longitude_start"] = row["longitude_start"]
		space["longitude_end"] 	 = row["longitude_end"]

		rider["gender"]			= 'M' if row["gender"] == 'Male' else 'F'
		rider["type"]			= row["usertype"]

		normalized_row["time"] 	= time
		normalized_row["city"] 	= city
		normalized_row["space"] = space
		normalized_row["rider"] = rider

		print(json.dumps(normalized_row))

package com.trivia.multi.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.trivia.multi.data.model.Question

object Mock {

    fun buildQuestions() {
        val questions = listOf(
            Question("What is the capital of France?", "Paris", listOf("Berlin", "Madrid", "Paris", "Rome")),
            Question("Who wrote 'To Kill a Mockingbird'?", "Harper Lee", listOf("Harper Lee", "J.K. Rowling", "Ernest Hemingway", "Mark Twain")),
            Question("What is the chemical symbol for gold?", "Au", listOf("Ag", "Au", "Pb", "Pt")),
            Question("Which planet is known as the Red Planet?", "Mars", listOf("Earth", "Mars", "Jupiter", "Saturn")),
            Question("Who painted the Mona Lisa?", "Leonardo da Vinci", listOf("Leonardo da Vinci", "Vincent van Gogh", "Pablo Picasso", "Claude Monet")),
            Question("What is the largest ocean on Earth?", "Pacific Ocean", listOf("Atlantic Ocean", "Indian Ocean", "Arctic Ocean", "Pacific Ocean")),
            Question("What is the smallest country in the world?", "Vatican City", listOf("Monaco", "Vatican City", "San Marino", "Liechtenstein")),
            Question("What is the main ingredient in guacamole?", "Avocado", listOf("Tomato", "Avocado", "Onion", "Garlic")),
            Question("Which animal is known as the King of the Jungle?", "Lion", listOf("Elephant", "Tiger", "Lion", "Giraffe")),
            Question("What is the chemical formula for water?", "H2O", listOf("CO2", "H2O", "O2", "NaCl")),
            Question("Who was the first President of the United States?", "George Washington", listOf("Thomas Jefferson", "George Washington", "Abraham Lincoln", "John Adams")),
            Question("In which year did World War II end?", "1945", listOf("1939", "1941", "1945", "1950")),
            Question("Who discovered penicillin?", "Alexander Fleming", listOf("Marie Curie", "Alexander Fleming", "Louis Pasteur", "Isaac Newton")),
            Question("What is the largest desert in the world?", "Sahara", listOf("Gobi", "Sahara", "Kalahari", "Atacama")),
            Question("Which planet is closest to the Sun?", "Mercury", listOf("Venus", "Earth", "Mercury", "Mars")),
            Question("What is the longest river in the world?", "Nile", listOf("Amazon", "Yangtze", "Nile", "Mississippi")),
            Question("Who invented the telephone?", "Alexander Graham Bell", listOf("Nikola Tesla", "Thomas Edison", "Alexander Graham Bell", "Samuel Morse")),
            Question("What is the hardest natural substance on Earth?", "Diamond", listOf("Gold", "Platinum", "Diamond", "Iron")),
            Question("What is the capital city of Japan?", "Tokyo", listOf("Beijing", "Seoul", "Tokyo", "Kyoto")),
            Question("Which ocean is the largest?", "Pacific Ocean", listOf("Atlantic Ocean", "Indian Ocean", "Southern Ocean", "Pacific Ocean")),
            Question("Which country is the largest producer of coffee?", "Brazil", listOf("Colombia", "Brazil", "Vietnam", "Mexico")),
            Question("What is the chemical symbol for oxygen?", "O", listOf("O", "O2", "O3", "O2O")),
            Question("Who invented the lightbulb?", "Thomas Edison", listOf("Nikola Tesla", "Alexander Graham Bell", "Thomas Edison", "Michael Faraday")),
            Question("Which country has the longest coastline?", "Canada", listOf("Australia", "Canada", "Russia", "USA")),
            Question("What is the currency of Japan?", "Yen", listOf("Won", "Yen", "Ringgit", "Rupee")),
            Question("What is the national flower of the United States?", "Rose", listOf("Tulip", "Rose", "Orchid", "Daisy")),
            Question("What does 'www' stand for in a website address?", "World Wide Web", listOf("Web World Wide", "World Wide Web", "Website World Web", "World Wide Web World")),
            Question("Who wrote '1984'?", "George Orwell", listOf("Aldous Huxley", "George Orwell", "Ray Bradbury", "Philip K. Dick")),
            Question("What is the largest land animal?", "Elephant", listOf("Elephant", "Giraffe", "Rhino", "Horse")),
            Question("Which gas do plants absorb from the atmosphere?", "Carbon Dioxide", listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Hydrogen")),
            Question("What is the chemical symbol for sodium?", "Na", listOf("Na", "N", "S", "O")),
            Question("Which animal is known for its ability to change color?", "Chameleon", listOf("Chameleon", "Octopus", "Cuttlefish", "Lizard")),
            Question("In what country would you find the Great Barrier Reef?", "Australia", listOf("Australia", "USA", "Mexico", "South Africa")),
            Question("What is the capital of Canada?", "Ottawa", listOf("Vancouver", "Toronto", "Ottawa", "Montreal")),
            Question("Which element has the chemical symbol 'Fe'?", "Iron", listOf("Iron", "Copper", "Gold", "Lead")),
            Question("What is the smallest continent?", "Australia", listOf("Asia", "Africa", "Australia", "Europe")),
            Question("In which city is the famous landmark, the Eiffel Tower?", "Paris", listOf("Paris", "London", "Berlin", "Rome")),
            Question("Who was the first man on the Moon?", "Neil Armstrong", listOf("Buzz Aldrin", "Neil Armstrong", "Yuri Gagarin", "John Glenn")),
            Question("What is the square root of 64?", "8", listOf("6", "8", "10", "12")),
            Question("What is the speed of light?", "299,792 km/s", listOf("150,000 km/s", "299,792 km/s", "1,000,000 km/s", "300,000 km/s")),
            Question("Who discovered America?", "Christopher Columbus", listOf("Christopher Columbus", "Leif Erikson", "Ferdinand Magellan", "Marco Polo")),
            Question("What is the capital of Italy?", "Rome", listOf("Venice", "Rome", "Florence", "Milan")),
            Question("Which country is famous for its pyramids?", "Egypt", listOf("Mexico", "Egypt", "India", "China")),
            Question("What is the largest planet in our Solar System?", "Jupiter", listOf("Earth", "Mars", "Saturn", "Jupiter")),
            Question("What is the chemical formula for methane?", "CH4", listOf("CH4", "CO2", "H2O", "C2H6")),
            Question("Which language is spoken in Brazil?", "Portuguese", listOf("Spanish", "Portuguese", "English", "French")),
            Question("What is the capital of Egypt?", "Cairo", listOf("Cairo", "Alexandria", "Giza", "Luxor")),
            Question("What is the main ingredient in sushi?", "Rice", listOf("Fish", "Rice", "Seaweed", "Egg")),
            Question("What is the largest island in the world?", "Greenland", listOf("Greenland", "New Guinea", "Borneo", "Madagascar")),
            Question("What is the symbol for the element with atomic number 79?", "Au", listOf("Au", "Ag", "Pb", "Pt")),
            Question("Which country is the largest in area?", "Russia", listOf("Canada", "USA", "China", "Russia"))
        )
        val firestore = FirebaseFirestore.getInstance()

            val questionsRef = firestore.collection("questions")

            // Start a batch to write multiple questions at once
            val batch = firestore.batch()

            // Iterate through the list of questions and add them to the batch
            questions.forEach { question ->
                val questionRef = questionsRef.document() // Automatically generates a new ID
                batch.set(questionRef, question) // Adds the question to the batch
            }

            // Commit the batch
            batch.commit()
                .addOnSuccessListener {
                    println("Questions successfully added to Firestore.")
                }
                .addOnFailureListener { e ->
                    println("Error adding questions: $e")
                }

    }
}
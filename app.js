document.addEventListener('DOMContentLoaded', ()=>{
  const form = document.getElementById('requirementsForm');
  const output = document.getElementById('jsonOutput');
  const copyBtn = document.getElementById('copyBtn');
  const downloadBtn = document.getElementById('downloadBtn');
  const resetBtn = document.getElementById('resetBtn');
  const sendBtn = document.createElement('button');
  sendBtn.textContent = 'Get Clarifying Questions';
  sendBtn.id = 'sendBtn';
  sendBtn.style.marginLeft = '8px';
  document.querySelector('.actions').appendChild(sendBtn);
  const refineBtn = document.getElementById('refineBtn');
  const amendmentsField = document.getElementById('amendments');
  
  // New elements for questions flow
  const questionsCard = document.getElementById('questionsCard');
  const questionsContainer = document.getElementById('questionsContainer');
  const questionsContext = document.getElementById('questionsContext');
  const skipQuestionsBtn = document.getElementById('skipQuestionsBtn');
  const submitAnswersBtn = document.getElementById('submitAnswersBtn');
  const itineraryDisplay = document.getElementById('itineraryDisplay');
  const itinerarySummary = document.getElementById('itinerarySummary');
  const dayPlansContainer = document.getElementById('dayPlansContainer');
  const bookingOptionsContainer = document.getElementById('bookingOptionsContainer');
  const eventsContainer = document.getElementById('eventsContainer');
  const weatherContainer = document.getElementById('weatherContainer');

  let currentQuestions = null;
  let currentItinerary = null;

  function getFormData(){
    const fd = new FormData(form);
    const interests = Array.from(document.getElementById('interests').selectedOptions).map(o=>o.value);
    const bookingPreferences = [];
    if(document.getElementById('pref_flight').checked) bookingPreferences.push('flight');
    if(document.getElementById('pref_train').checked) bookingPreferences.push('train');
    if(document.getElementById('pref_car').checked) bookingPreferences.push('car');
    if(document.getElementById('pref_bus').checked) bookingPreferences.push('bus');
    const data = {
      tripTitle: fd.get('tripTitle') || '',
      days: parseInt(fd.get('days')) || 1,
      tentativeDates: fd.get('tentativeDates') || '',
      region: fd.get('region') || '',
      budget: fd.get('budget') || '',
      people: parseInt(fd.get('people')) || 1,
      special: {
        kids: !!fd.get('kids'),
        elderly: !!fd.get('elderly'),
        differentlyAbled: !!fd.get('differentlyAbled')
      },
      weatherPreference: fd.get('weather') || 'any',
      foodPreferences: (fd.get('food')||'').split(',').map(s=>s.trim()).filter(Boolean),
      bookingPreferences: bookingPreferences,
      interests: interests,
      notes: fd.get('notes') || ''
    };

    return data;
  }

  function renderJSON(data){
    output.textContent = JSON.stringify(data, null, 2);
  }

  function displayQuestions(questionResponse) {
    currentQuestions = questionResponse;
    questionsContext.textContent = questionResponse.context || 'Please answer these questions to help us create a better itinerary for you.';
    
    questionsContainer.innerHTML = '';
    questionResponse.questions.forEach((question, index) => {
      const questionDiv = document.createElement('div');
      questionDiv.className = 'question-item';
      
      const label = document.createElement('label');
      label.textContent = question.question;
      if (question.required) {
        label.innerHTML += ' <span class="required">*</span>';
      }
      
      let input;
      if (question.options && question.options.length > 0) {
        input = document.createElement('select');
        input.id = `question_${index}`;
        input.name = `question_${index}`;
        
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Select an option...';
        input.appendChild(defaultOption);
        
        question.options.forEach(option => {
          const optionElement = document.createElement('option');
          optionElement.value = option;
          optionElement.textContent = option;
          input.appendChild(optionElement);
        });
      } else {
        input = document.createElement('textarea');
        input.id = `question_${index}`;
        input.name = `question_${index}`;
        input.rows = 2;
        input.placeholder = 'Your answer...';
      }
      
      questionDiv.appendChild(label);
      questionDiv.appendChild(input);
      questionsContainer.appendChild(questionDiv);
    });
    
    questionsCard.style.display = 'block';
    questionsCard.scrollIntoView({ behavior: 'smooth' });
  }

  function getQuestionAnswers() {
    const answers = {};
    currentQuestions.questions.forEach((question, index) => {
      const input = document.getElementById(`question_${index}`);
      if (input) {
        answers[question.question] = input.value;
      }
    });
    return answers;
  }

  function displayItinerary(itinerary) {
    currentItinerary = itinerary;
    
    // Show itinerary display and hide JSON initially
    itineraryDisplay.style.display = 'block';
    output.style.display = 'none';
    
    // Display summary
    itinerarySummary.innerHTML = `<h3>${itinerary.summary}</h3>`;
    
    // Display day plans
    dayPlansContainer.innerHTML = '<h3>Daily Itinerary</h3>';
    if (itinerary.dayPlans && itinerary.dayPlans.length > 0) {
      itinerary.dayPlans.forEach(dayPlan => {
        const dayDiv = document.createElement('div');
        dayDiv.className = 'day-plan';
        dayDiv.innerHTML = `
          <div class="day-header">
            <h4>Day ${dayPlan.dayNumber}: ${dayPlan.title}</h4>
          </div>
          <div class="day-activities">
            ${dayPlan.activities.map(activity => `
              <div class="activity">
                <div class="activity-time">${activity.time || ''}</div>
                <div class="activity-details">
                  <strong>${activity.title}</strong>
                  ${activity.description ? `<p>${activity.description}</p>` : ''}
                  ${activity.location ? `<p><em>Location: ${activity.location}</em></p>` : ''}
                  ${activity.cost ? `<p><strong>Cost: ${activity.cost}</strong></p>` : ''}
                </div>
              </div>
            `).join('')}
          </div>
        `;
        dayPlansContainer.appendChild(dayDiv);
      });
    }
    
    // Display booking options
    bookingOptionsContainer.innerHTML = '<h3>Booking Options</h3>';
    if (itinerary.bookings) {
      if (itinerary.bookings.flights && itinerary.bookings.flights.options) {
        bookingOptionsContainer.innerHTML += '<h4>Flights</h4>';
        itinerary.bookings.flights.options.forEach((flight, index) => {
          bookingOptionsContainer.innerHTML += `
            <div class="booking-option">
              <h5>${flight.carrier} - ${flight.price}</h5>
              <p>Departure: ${flight.departureTime} | Arrival: ${flight.arrivalTime}</p>
              <p>Duration: ${flight.duration} | Stops: ${flight.stops}</p>
              <div class="pros-cons">
                <div class="pros"><strong>Pros:</strong> ${flight.pros ? flight.pros.join(', ') : 'N/A'}</div>
                <div class="cons"><strong>Cons:</strong> ${flight.cons ? flight.cons.join(', ') : 'N/A'}</div>
              </div>
              <button class="select-btn" onclick="selectOption('flight', ${index})">Select</button>
            </div>
          `;
        });
      }
      
      if (itinerary.bookings.hotels && itinerary.bookings.hotels.options) {
        bookingOptionsContainer.innerHTML += '<h4>Hotels</h4>';
        itinerary.bookings.hotels.options.forEach((hotel, index) => {
          bookingOptionsContainer.innerHTML += `
            <div class="booking-option">
              <h5>${hotel.name} - ${hotel.pricePerNight}</h5>
              <p>Location: ${hotel.location} | Rating: ${hotel.rating}</p>
              <p>Amenities: ${hotel.amenities ? hotel.amenities.join(', ') : 'N/A'}</p>
              <div class="pros-cons">
                <div class="pros"><strong>Pros:</strong> ${hotel.pros ? hotel.pros.join(', ') : 'N/A'}</div>
                <div class="cons"><strong>Cons:</strong> ${hotel.cons ? hotel.cons.join(', ') : 'N/A'}</div>
              </div>
              <button class="select-btn" onclick="selectOption('hotel', ${index})">Select</button>
            </div>
          `;
        });
      }
    }
    
    // Display events
    if (itinerary.events && itinerary.events.length > 0) {
      eventsContainer.innerHTML = '<h3>Available Events & Activities</h3>';
      itinerary.events.forEach(event => {
        eventsContainer.innerHTML += `
          <div class="event-item">
            <h5>${event.name}</h5>
            <p><strong>Date:</strong> ${event.date} at ${event.time}</p>
            <p><strong>Location:</strong> ${event.location}</p>
            <p><strong>Description:</strong> ${event.description}</p>
            <p><strong>Category:</strong> ${event.category} | <strong>Price:</strong> ${event.price}</p>
          </div>
        `;
      });
    }
    
    // Display weather
    if (itinerary.weather) {
      weatherContainer.innerHTML = '<h3>Weather Forecast</h3>';
      weatherContainer.innerHTML += `<p><strong>Summary:</strong> ${itinerary.weather.forecastSummary || 'N/A'}</p>`;
      
      if (itinerary.weather.dailyForecast) {
        weatherContainer.innerHTML += '<h4>Daily Forecast</h4>';
        itinerary.weather.dailyForecast.forEach(day => {
          weatherContainer.innerHTML += `
            <div class="weather-day">
              <strong>${day.date}</strong>: ${day.condition} | High: ${day.high} | Low: ${day.low}
              <p>Recommendations: ${day.recommendations ? day.recommendations.join(', ') : 'N/A'}</p>
            </div>
          `;
        });
      }
      
      if (itinerary.weather.packingSuggestions) {
        weatherContainer.innerHTML += `<p><strong>Packing Suggestions:</strong> ${itinerary.weather.packingSuggestions.join(', ')}</p>`;
      }
    }
    
    // Show JSON output as well
    output.style.display = 'block';
    renderJSON(itinerary);
  }

  // Global function for option selection
  window.selectOption = function(type, index) {
    console.log(`Selected ${type} option ${index}`);
    // You can implement selection logic here
    alert(`Selected ${type} option ${index + 1}`);
  };

  form.addEventListener('submit', (e)=>{
    e.preventDefault();
    const data = getFormData();
    renderJSON(data);
  });

  copyBtn.addEventListener('click', async ()=>{
    try{
      await navigator.clipboard.writeText(output.textContent);
      copyBtn.textContent = 'Copied!';
      setTimeout(()=>copyBtn.textContent='Copy JSON', 1500);
    }catch(err){
      console.error(err);
      alert('Copy failed — please select and copy manually.');
    }
  });

  downloadBtn.addEventListener('click', ()=>{
    const blob = new Blob([output.textContent], {type:'application/json'});
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'trip-itinerary.json';
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  });

  resetBtn.addEventListener('click', ()=>{
    form.reset();
    renderJSON({});
    questionsCard.style.display = 'none';
    itineraryDisplay.style.display = 'none';
    output.style.display = 'block';
    currentQuestions = null;
    currentItinerary = null;
  });

  sendBtn.addEventListener('click', async ()=>{
    const data = getFormData();
    try{
      const resp = await fetch('http://localhost:8080/api/itineraries/questions', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify(data)
      });
      if(!resp.ok) throw new Error('Server error: '+resp.status);
      const questionResponse = await resp.json();
      displayQuestions(questionResponse);
    }catch(err){
      alert('Failed to get questions: '+err.message);
    }
  });

  skipQuestionsBtn.addEventListener('click', async ()=>{
    const data = getFormData();
    await generateItinerary(data);
  });

  submitAnswersBtn.addEventListener('click', async ()=>{
    const data = getFormData();
    const answers = getQuestionAnswers();
    
    // Add answers to the request data
    data.questionAnswers = answers;
    
    await generateItinerary(data);
  });

  async function generateItinerary(data) {
    try{
      const resp = await fetch('http://localhost:8080/api/itineraries', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify(data)
      });
      if(!resp.ok) throw new Error('Server error: '+resp.status);
      const itinerary = await resp.json();
      displayItinerary(itinerary);
      questionsCard.style.display = 'none';
    }catch(err){
      alert('Failed to generate itinerary: '+err.message);
    }
  }

  // Refine flow: send current displayed itinerary as previousItinerary along with amendments
  refineBtn && refineBtn.addEventListener('click', async ()=>{
    const amendmentsText = amendmentsField.value || '';
    if (!amendmentsText.trim()) {
      alert('Please enter amendments or feedback to refine the itinerary.');
      return;
    }

    const payload = Object.assign(getFormData(), { 
      amendments: amendmentsText, 
      previousItinerary: currentItinerary 
    });

    try{
      const resp = await fetch('http://localhost:8080/api/itineraries', {
        method: 'POST', 
        headers: {'Content-Type':'application/json'}, 
        body: JSON.stringify(payload)
      });
      if(!resp.ok) throw new Error('Server error: '+resp.status);
      const refined = await resp.json();
      displayItinerary(refined);
    }catch(err){ 
      alert('Refine failed: '+err.message); 
    }
  });

  // Render default state
  renderJSON({});
});

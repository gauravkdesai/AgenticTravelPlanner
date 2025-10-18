document.addEventListener('DOMContentLoaded', ()=>{
  const form = document.getElementById('requirementsForm');
  const output = document.getElementById('jsonOutput');
  const copyBtn = document.getElementById('copyBtn');
  const downloadBtn = document.getElementById('downloadBtn');
  const resetBtn = document.getElementById('resetBtn');
  const sendBtn = document.createElement('button');
  sendBtn.textContent = 'Send to backend';
  sendBtn.id = 'sendBtn';
  sendBtn.style.marginLeft = '8px';
  document.querySelector('.actions').appendChild(sendBtn);
  const refineBtn = document.getElementById('refineBtn');
  const amendmentsField = document.getElementById('amendments');

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
    a.download = 'trip-requirements.json';
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  });

  resetBtn.addEventListener('click', ()=>{
    form.reset();
    renderJSON({});
  });

  sendBtn.addEventListener('click', async ()=>{
    const data = getFormData();
    try{
      const resp = await fetch('http://localhost:8080/api/itineraries', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify(data)
      });
      if(!resp.ok) throw new Error('Server error: '+resp.status);
      const itinerary = await resp.json();
      // show itinerary in output pane
      renderJSON(itinerary);
    }catch(err){
      alert('Failed to send to backend: '+err.message);
    }
  });

  // Refine flow: send current displayed itinerary as previousItinerary along with amendments
  refineBtn && refineBtn.addEventListener('click', async ()=>{
    const amendmentsText = amendmentsField.value || '';
    // try parse current output as previousItinerary
    let previousItinerary = null;
    try{ previousItinerary = JSON.parse(output.textContent); }catch(e){ previousItinerary = null; }

    const payload = Object.assign(getFormData(), { amendments: amendmentsText, previousItinerary });

    try{
      const resp = await fetch('http://localhost:8080/api/itineraries', {
        method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload)
      });
      if(!resp.ok) throw new Error('Server error: '+resp.status);
      const refined = await resp.json();
      renderJSON(refined);
    }catch(err){ alert('Refine failed: '+err.message); }
  });

  // Render default state
  renderJSON({});
});

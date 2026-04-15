import "./App.scss";
import MigrationUploadSection from "./components/MigrationUploadSection";
import ServiceDescription from "./components/ServiceDescription";

function App() {
  return (
    <main className="page">
      <ServiceDescription />
      <MigrationUploadSection />
    </main>
  );
}

export default App;
